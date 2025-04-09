package util;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.postgresql.util.PSQLException;

import dao.DAOHelper;
import dao.DaoFactory;
import enums.Constants.AggregateFunction;
import enums.Constants.HttpStatusCodes;
import enums.Constants.Operators;
import model.ColumnCriteria;
import model.Criteria;
import model.JoinObject;
import util.ColumnYamlUtil.ClassMapping;
import util.ColumnYamlUtil.FieldMapping;

public class SQLHelper {

	private static Logger logger = LogManager.getLogger(SQLHelper.class);

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> T mapResultSetToObject(ResultSet resultSet, Class<? extends T> clazz, T instance,
			String tableName, List<String> modifiedFields) throws CustomException {
		try {
			if (instance == null) {
				instance = clazz.getDeclaredConstructor().newInstance();
			}
			ClassMapping classMapping = ColumnYamlUtil.getMapping(clazz.getName());
			if (classMapping == null) {
				logger.error("Mapping for class {} not found.", clazz);
				throw new CustomException("Requested data could not be found. Please contact support.",
						HttpStatusCodes.NOT_FOUND);
			}

			Map<String, FieldMapping> fieldMap = classMapping.getFields();
			Field[] fields = clazz.getDeclaredFields();
			for (Field field : fields) {
				try {
					FieldMapping fieldMapping = fieldMap.get(field.getName());
					if (fieldMapping == null) {
						continue;
					}
					field.setAccessible(true);
					String columnName = fieldMapping.getColumnName();
					Object columnValue = null;
					try {
						columnValue = resultSet.getObject(columnName);
						if (resultSet.wasNull()) {
							continue;
						}
					} catch (SQLSyntaxErrorException e) {
						logger.warn("Column '{}' not found in the result set, skipping.", columnName);
					} catch (PSQLException e) {
						if (e.getMessage().contains("column does not exist")) {
							logger.warn("PostgreSQL: Column '{}' not found in the result set, skipping.", columnName);
						} else {
							logger.error("PostgreSQL Error accessing column '{}': {}", columnName, e.getMessage());
						}
					}
					if (field.getType().isEnum() && columnValue instanceof String) {
						String enumValue = (String) columnValue;
						Object enumConstant = Enum.valueOf((Class<Enum>) field.getType(), enumValue);
						field.set(instance, enumConstant);
					} else {
						field.set(instance, columnValue);
					}
					modifiedFields.add(columnName);
				} catch (SQLSyntaxErrorException exception) {
					logger.warn("SQL Syntax error while setting field: " + field.getName(), exception);
					throw new CustomException("Error processing data. Please try again later.",
							HttpStatusCodes.INTERNAL_SERVER_ERROR);
				} catch (IllegalAccessException e) {
					logger.error("Error accessing field: " + field.getName(), e);
					throw new CustomException("Error processing data. Please try again later.",
							HttpStatusCodes.INTERNAL_SERVER_ERROR);
				}
			}

			Class<?> superclass = clazz.getSuperclass();
			if (superclass != null && !superclass.getName().equals("model.MarkedClass")) {
				classMapping = ColumnYamlUtil.getMapping(superclass.getName());
				mapResultSetToObject(resultSet, (Class<? extends T>) superclass, instance, tableName, modifiedFields);
			}

			return instance;
		} catch (Exception e) {
			logger.error("Error mapping result set to object: ", e);
			throw new CustomException("Error processing data. Please try again later.",
					HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

	// Returns the preparedStatement after setting the values
	private static PreparedStatement getPreparedStatement(Connection connection, String query, Object[] values)
			throws CustomException, SQLException {
		Helper.checkNullValues(query);
		if (connection == null || connection.isClosed()) {
			throw new CustomException("Database connection is not established or is closed.",
					HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}

		PreparedStatement preparedStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
		if (values != null) {
			for (int i = 0; i < values.length; i++) {
				Object value = values[i];
				preparedStatement.setObject(i + 1, value);
			}
		}
		logger.debug("PreparedStatement: " + preparedStatement.toString());
		return preparedStatement;
	}

	// Execute the preparedStatement for the insert queries
	private static Object executeNonSelect(Connection connection, String query, Object[] values)
			throws CustomException {
		try (PreparedStatement preparedStatement = getPreparedStatement(connection, query, values)) {
			preparedStatement.execute();
			try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
				if (generatedKeys.next()) {
					return generatedKeys.getObject(1);
				}
			}
		} catch (SQLIntegrityConstraintViolationException e) {
			String message = DAOHelper.parseDuplicateEntryMessage(e.getMessage());
			try {
				if (connection != null && !connection.isClosed()) {
					connection.rollback();
					logger.info("Transaction rolled back due to error.");
				}
			} catch (Exception rollbackException) {
				logger.error("Rollback failed for the transaction. Query could not be rolled back. Error details: {}",
						rollbackException.getMessage(), rollbackException);
				throw new CustomException("Transaction failed. Please try again later.",
						HttpStatusCodes.INTERNAL_SERVER_ERROR);
			}
			logger.info("Duplicate entry error: ", e);
			throw new CustomException(message, HttpStatusCodes.CONFLICT);
		} catch (SQLException e) {
			Helper.handleSQLException(e);
		}
		return null;
	}

	// Execute the preparedStatement for the non-select queries.
	private static Object executeNonSelect(String query, Object[] values) throws CustomException {
		DBConnection dbConnection = DBConnection.getInstance();
		try (Connection connection = dbConnection.getConnection();) {
			return executeNonSelect(connection, query, values);
		} catch (SQLException e) {
			Helper.handleSQLException(e);
		} catch (InterruptedException e) {
			throw new CustomException("Unexpected error occurred.", HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
		return null;
	}

	// @AppendQuery method
	// sql : sql string to be appended.
	// conditions : It contains the criteria that has to be included in that query
	// (WHERE clause).
	// conditionValues : It contains the values of the placeholder(?) in the query.
	public static <T> void QueryBuilder(StringBuilder sql, Criteria condition, List<Object> conditionValues)
			throws CustomException {

		if (condition == null) {
			logger.error("Condition object is null. The request is invalid.");
			throw new CustomException("Invalid request. Please check the input and try again.",
					HttpStatusCodes.BAD_REQUEST);
		}

		// Ensure the join column and join value lists are the same size
		if (condition.getJoinColumn() != null && condition.getJoinColumn().size() != condition.getJoinValue().size()) {
			logger.error("Join columns and values size mismatch. Columns: {}, Values: {}", condition.getJoinColumn(),
					condition.getJoinValue());
			throw new CustomException("Invalid configuration. Please check the input and try again.",
					HttpStatusCodes.BAD_REQUEST);
		}

		List<String> joinColumn = condition.getJoinColumn(), joinOperator = condition.getJoinOperator(),
				joinTable = condition.getJoinTable();
		List<Object> joinValue = condition.getJoinValue();

		if (joinColumn != null && !joinColumn.isEmpty()) {
			for (int i = 0; i < joinColumn.size(); i++) {
				Operators opEnum = Operators.valueOf(joinOperator.get(i).toUpperCase());
				sql.append(condition.getJoin()).append(joinTable.get(i)).append(" ON ").append(joinColumn.get(i))
						.append(" ").append(opEnum.getSymbol()).append(" ").append(joinValue.get(i));
			}
		}
		List<String> columns = condition.getColumn(), operators = condition.getOperator();
		List<Object> columnvalues = condition.getValue();
		if (!columns.isEmpty()) {
			sql.append(" WHERE ");
		}
		for (int i = 0; i < columns.size(); i++) {
			String column = columns.get(i);
			String operator = operators.get(i);
			Object value = null;
			if (!columnvalues.isEmpty()) {
				value = columnvalues.get(i);
			}
			sql.append(column).append(" ");

			try {
				Operators opEnum = Operators.valueOf(operator.toUpperCase());

				switch (opEnum) {
				case IN:
					DAOHelper.appendInClause(sql, condition, conditionValues);
					break;
				case BETWEEN:
					DAOHelper.appendBetweenClause(sql, condition, conditionValues);
					break;
				case LIKE:
					switch (DaoFactory.CURRENT_DB) {
					case MYSQL:
						sql.append("LIKE ?");
						conditionValues.add(value);
						break;
					case POSTGRESQL:
						int start = sql.length() - column.length() - 1, end = sql.length();
						sql.delete(start, end);
						sql.append("CAST(").append(column).append(" AS TEXT) LIKE ?");
						conditionValues.add(value);
						break;
					}
					break;
				case NOT:
					sql.append("NOT ");
					break;
				case LESS_THAN:
				case GREATER_THAN:
				case LESS_THAN_OR_EQUAL_TO:
				case GREATER_THAN_OR_EQUAL_TO:
				case EQUAL_TO:
				case NOT_EQUAL_TO:
					DAOHelper.appendComparisonOperator(sql, opEnum.getSymbol(), value, conditionValues);
					break;
				default:
					throw new CustomException("Unsupported operator: " + operator, HttpStatusCodes.BAD_REQUEST);
				}
			} catch (IllegalArgumentException e) {
				throw new CustomException("Invalid operator: " + operator, e, HttpStatusCodes.BAD_REQUEST);
			}

			if (condition.getLogicalOperator() != null && i < columns.size() - 1) {
				sql.append(" ").append(condition.getLogicalOperator()).append(" ");
			}
		}
		if (!"COUNT".equalsIgnoreCase(condition.getAggregateFunction())) {
			if (condition.getOrderBy() != null) {
				sql.append(" ORDER BY ").append(condition.getOrderByField()).append(" ").append(condition.getOrderBy());
			}
			if (condition.getLimitValue() != null) {
				sql.append(" LIMIT ").append(condition.getLimitValue());
			}
		}
		if (condition.getOffsetValue() != null && condition.getOffsetValue() >= 0) {
			sql.append(" OFFSET ?");
			conditionValues.add(condition.getOffsetValue());
		}
		logger.debug("Generated Query: " + sql.toString());
	}

	// @Update method
	// table : name of the table.
	// columnCriteriaList : it contains the column and value to be updated.
	// conditions : It contains the criteria that has to be included in that query
	// (WHERE clause).
	public static <T> void update(ColumnCriteria columnCriteriaList, Criteria criterias)
			throws CustomException, SQLException {

		Helper.checkNullValues(criterias);
		Helper.checkNullValues(columnCriteriaList);

		Class<?> clazz = criterias.getClazz();
		ClassMapping classMapping = ColumnYamlUtil.getMapping(clazz.getName());
		Helper.validateClassMapping(clazz, classMapping);

		String table = classMapping.getTableName();
		Helper.validateTableName(table, clazz.getName());

		StringBuilder updateSql = new StringBuilder("UPDATE " + table + " SET ");
		List<Object> values = new ArrayList<>();
		List<Object> setValues = columnCriteriaList.getValues();
		List<String> setColumns = columnCriteriaList.getFields();

		Helper.validateQueryConditions(setColumns, "No columns to update.");

		int length = updateSql.length();

		logger.info("size: " + setColumns.size());
		for (int i = 0; i < setColumns.size(); i++) {
			String setColumn = setColumns.get(i);
			Object setValue = setValues.get(i);
			FieldMapping fieldMapping = classMapping.getFields().get(setColumn);

			logger.info("setColumn: " + setColumn);
			logger.info("fieldMapping: " + fieldMapping);
			if (fieldMapping != null) {
				if (length < updateSql.length()) {
					updateSql.append(", ");
					length = updateSql.length();
				}
				updateSql.append(fieldMapping.getColumnName()).append(" = ?");
				values.add(setValue);
			}
		}

		logger.info("values: " + values);
		if (!values.isEmpty()) {
			if (classMapping.getReferedField() != null) {
				criterias.setColumn(Arrays.asList(classMapping.getReferedField()));
			}
			QueryBuilder(updateSql, criterias, values);
			logger.info(updateSql);
			executeNonSelect(updateSql.toString(), values.toArray());
		}

		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null && !superclass.getName().equals("model.MarkedClass")) {
			criterias.setClazz(superclass);
			update(columnCriteriaList, criterias);
		}
	}

	// @Delete method
	// table : name of the table.
	// conditions : It contains the criteria that has to be included in that query
	// (WHERE clause).
	public static <T> void delete(Criteria conditions) throws CustomException, SQLException {
		Helper.checkNullValues(conditions);
		Class<?> clazz = conditions.getClazz();
		ClassMapping classMapping = ColumnYamlUtil.getMapping(clazz.getName());
		Helper.validateClassMapping(clazz, classMapping);

		String table = classMapping.getTableName();
		Helper.validateTableName(table, clazz.getName());
		StringBuilder deleteSql = new StringBuilder("DELETE FROM ").append(table);
		List<Object> values = new ArrayList<>();
		QueryBuilder(deleteSql, conditions, values);
		executeNonSelect(deleteSql.toString(), values.toArray());
	}

	// @Get method
	// clazz : class of the pojo to be returned.
	// condition : It contains the criteria that has to be included in that query

	public static <T> List<T> get(Criteria condition, Class<T> clazz)
			throws CustomException, SQLException, InterruptedException {
		List<Object> conditionValues = new ArrayList<>();
		StringBuilder selectSql = buildSelectQuery(condition, clazz, conditionValues);
		DBConnection dbConnection = DBConnection.getInstance();

		try (Connection connection = dbConnection.getConnection();
				PreparedStatement preparedStatement = getPreparedStatement(connection, selectSql.toString(),
						conditionValues.toArray());
				ResultSet resultSet = preparedStatement.executeQuery()) {

			List<T> resultList = new ArrayList<>();
			while (resultSet.next()) {
				T instance = mapResultSetToObject(resultSet, clazz, null, getTableName(clazz), new ArrayList<>());
				resultList.add(instance);
			}
			return resultList;
		}
	}

	public static <T> List<JoinObject<T>> getJoinedObjects(Criteria condition, Class<T> clazz)
			throws CustomException, SQLException, InterruptedException {
		List<Object> conditionValues = new ArrayList<>();
		StringBuilder selectSql = buildSelectQuery(condition, clazz, conditionValues);
		DBConnection dbConnection = DBConnection.getInstance();
		try (Connection connection = dbConnection.getConnection();
				PreparedStatement preparedStatement = getPreparedStatement(connection, selectSql.toString(),
						conditionValues.toArray());
				ResultSet resultSet = preparedStatement.executeQuery()) {

			List<JoinObject<T>> resultList = new ArrayList<>();
			while (resultSet.next()) {
				List<String> modifiedFields = new ArrayList<>();
				T instance = mapResultSetToObject(resultSet, clazz, null, getTableName(clazz), modifiedFields);

				JoinObject<T> joinInstance = mapLeftOverResultSet(resultSet, modifiedFields, instance, clazz);
				resultList.add(joinInstance);
			}
			return resultList;
		}
	}

	public static <T> Long getCount(Criteria condition, Class<T> clazz)
			throws CustomException, SQLException, InterruptedException {
		List<Object> conditionValues = new ArrayList<>();
		StringBuilder selectSql = buildSelectQuery(condition, clazz, conditionValues);
		DBConnection dbConnection = DBConnection.getInstance();
		try (Connection connection = dbConnection.getConnection();
				PreparedStatement preparedStatement = getPreparedStatement(connection, selectSql.toString(),
						conditionValues.toArray());
				ResultSet resultSet = preparedStatement.executeQuery()) {
			if (resultSet.next()) {
				return resultSet.getLong(1);
			}
			return 0l;
		}
	}

	private static <T> String getTableName(Class<T> clazz) throws CustomException {
		ClassMapping classMapping = ColumnYamlUtil.getMapping(clazz.getName());
		Helper.validateClassMapping(clazz, classMapping);
		return classMapping.getTableName();
	}

	private static <T> StringBuilder buildSelectQuery(Criteria condition, Class<T> clazz, List<Object> conditionValues)
			throws CustomException {
		String tableName = getTableName(clazz);
		Helper.validateTableName(tableName, clazz.getName());

		List<String> selectColumns = condition.getSelectColumn();
		Helper.validateQueryConditions(selectColumns, "No columns to select.");

		StringBuilder selectSql = new StringBuilder("SELECT ");
		if (condition.getOffsetValue() != null && condition.getOffsetValue() == -1) {
			String aggregateFunction = condition.getAggregateFunction();
			String aggregateOperator = condition.getAggregateOperator();

			selectSql.append(AggregateFunction.get(aggregateFunction, aggregateOperator));
		} else {
			List<String> aliasStrings = condition.getAlias();
			for (int i = 0; i < selectColumns.size(); i++) {
				selectSql.append(selectColumns.get(i));
				if (aliasStrings != null && aliasStrings.get(i) != null
						&& aliasStrings.size() == selectColumns.size()) {
					selectSql.append(" AS " + aliasStrings.get(i));
				}
				if (i < selectColumns.size() - 1) {
					selectSql.append(", ");
				}
			}
		}
		selectSql.append(" FROM ").append(tableName);

		if (condition.getColumn() != null) {
			QueryBuilder(selectSql, condition, conditionValues);
		}

		return selectSql;
	}

	private static <T> JoinObject<T> mapLeftOverResultSet(ResultSet resultSet, List<String> modifiedFields, T instance,
			Class<T> clazz) throws CustomException {
		try {
			ResultSetMetaData metaData = resultSet.getMetaData();
			int columnCount = metaData.getColumnCount();

			JoinObject<T> joininstance = new JoinObject<T>(instance);
			for (int i = 1; i <= columnCount; i++) {
				String columnName = metaData.getColumnLabel(i);
				if (modifiedFields.contains(columnName)) {
					continue;
				}
				joininstance.addjoinedField(columnName, resultSet.getObject(columnName));
			}
			return joininstance;
		} catch (Exception e) {
			logger.error("Error mapping leftover result set: ", e);
			throw new CustomException("Error mapping leftover result set: " + e.getMessage(), e,
					HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}

	}

	// @Insert method
	// table : name of the table.
	// pojo : pojo class
	public static Object insert(Object pojo) throws Exception {
		Helper.checkNullValues(pojo);
		DBConnection dbConnection = DBConnection.getInstance();
		try (Connection connection = dbConnection.getConnection()) {
			if (connection == null || connection.isClosed()) {
				throw new CustomException("Database connection is not established or is closed.",
						HttpStatusCodes.INTERNAL_SERVER_ERROR);
			}
			connection.setAutoCommit(false);
			List<Class<?>> classList = getClassHierarchy(pojo.getClass());
			Object generatedValue = null;

			for (int k = classList.size() - 1; k >= 0; k--) {
				Class<?> clazz = classList.get(k);
				ClassMapping classMapping = ColumnYamlUtil.getMapping(clazz.getName());
				Map<String, FieldMapping> fieldMap = classMapping.getFields();
				String tableName = classMapping.getTableName();

				Field[] fields = clazz.getDeclaredFields();
				StringBuilder insertSql = buildInsertSQL(fields, classMapping, tableName, pojo, fieldMap);
				List<Object> values = collectFieldValues(fields, pojo, classMapping, fieldMap, generatedValue);

				Object incrementValue = executeNonSelect(connection, insertSql.toString(), values.toArray());
				if (k == classList.size() - 1) {
					generatedValue = incrementValue;
				}
			}
			connection.commit();
			connection.setAutoCommit(true);
			return generatedValue;
		}
	}

	private static List<Class<?>> getClassHierarchy(Class<?> clazz) {
		List<Class<?>> classList = new ArrayList<>();
		while (!clazz.getName().equals("model.MarkedClass")) {
			classList.add(clazz);
			clazz = clazz.getSuperclass();
		}
		return classList;
	}

	private static StringBuilder buildInsertSQL(Field[] fields, ClassMapping classMapping, String tableName,
			Object pojo, Map<String, FieldMapping> fieldMap) throws CustomException {
		StringBuilder insertSql = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
		int ctr = 0;
		for (Field field : fields) {
			if (isAutoIncrementField(classMapping, field)) {
				continue;
			}
			field.setAccessible(true);
			FieldMapping fieldMapping = fieldMap.get(field.getName());
			if (fieldMapping == null) {
				continue;
			}
			insertSql.append(fieldMapping.getColumnName()).append(", ");
			ctr++;
		}
		if (ctr == 0) {
			throw new CustomException("No valid fields found for insertion.", HttpStatusCodes.BAD_REQUEST);
		}
		insertSql.deleteCharAt(insertSql.length() - 2);
		insertSql.append(") VALUES (");
		for (int i = 0; i < ctr; i++) {
			insertSql.append("?");
			if (i < ctr - 1) {
				insertSql.append(", ");
			}
		}
		insertSql.append(");");
		return insertSql;
	}

	private static List<Object> collectFieldValues(Field[] fields, Object pojo, ClassMapping classMapping,
			Map<String, FieldMapping> fieldMap, Object generatedValue) throws CustomException {
		List<Object> values = new ArrayList<>();
		for (Field field : fields) {
			if (isAutoIncrementField(classMapping, field))
				continue;
			field.setAccessible(true);
			try {
				FieldMapping fieldMapping = fieldMap.get(field.getName());
				if (fieldMapping == null)
					continue;
				if (generatedValue != null && classMapping.getReferenceField() != null
						&& classMapping.getReferenceField().equals(field.getName())) {
					values.add(generatedValue);
				} else {
					Object fieldValue = field.get(pojo);
					if (field.getType().isEnum() && fieldValue != null) {
						values.add(((Enum<?>) fieldValue).name());
					} else {
						values.add(fieldValue);
					}
				}
			} catch (IllegalAccessException e) {
				throw new CustomException("Error accessing field value: " + e.getMessage(), e,
						HttpStatusCodes.INTERNAL_SERVER_ERROR);
			}
		}
		return values;
	}

	private static boolean isAutoIncrementField(ClassMapping classMapping, Field field) {
		return classMapping.getAutoIncrementField() != null
				&& classMapping.getAutoIncrementField().equals(field.getName());
	}

}