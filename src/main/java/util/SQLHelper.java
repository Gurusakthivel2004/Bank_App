package util;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import dblayer.connect.DBConnection;
import dblayer.model.ColumnCriteria;
import dblayer.model.Criteria;
import dblayer.model.MarkedClass;
import util.ColumnYamlUtil.ClassMapping;
import util.ColumnYamlUtil.FieldMapping;

public class SQLHelper {
	
	// map the column name to field name of the pojo class
	@SuppressWarnings("unchecked")
	private static <T> T mapResultSetToObject(ResultSet resultSet, Class<? extends T> clazz, T instance, String tableName) throws CustomException {
		try {
            if(instance == null) {
            	instance = clazz.getDeclaredConstructor().newInstance();
            }
            ClassMapping classMapping = ColumnYamlUtil.getMapping(clazz.getName());
    	    Map<String, FieldMapping> fieldMap = classMapping.getFields();
    	    Field[] fields = clazz.getDeclaredFields();
    	    for(Field field: fields) {
    	    	try {
    	    		FieldMapping fieldMapping = fieldMap.get(field.getName());
        	    	if(fieldMapping == null) {
                		continue;
                	}
        	    	field.setAccessible(true);
        	    	String columnName = fieldMapping.getColumnName();
        	    	Object columnValue = resultSet.getObject(columnName);
                    field.set(instance, columnValue);
    	    	} catch(SQLSyntaxErrorException exception) {
    	    		continue;
    	    	}
    	    }
            Class<?> superclass = clazz.getSuperclass();
            if (superclass != null && !superclass.getName().equals("dblayer.model.MarkedClass")) {
            	classMapping = ColumnYamlUtil.getMapping(superclass.getName());
                String superClassTableName = classMapping.getTableName();
                mapResultSetToObject(resultSet,(Class <? extends T>) superclass, instance, superClassTableName);
            }
            return instance;
        } catch (Exception e) {
            throw new CustomException("Error mapping result set to object: " + e.getMessage());
        } 
    }
	
	
	// Returns the preparedStatement after setting the values.
	private static PreparedStatement getPreparedStatement(Connection connection, String query, Object[] values) throws CustomException, SQLException {
		Helper.checkNullValues(query);
		PreparedStatement preparedStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
		if(values != null) {
			for(int i=0;i<values.length;i++) {
	    		Object value = values[i];
	    		preparedStatement.setObject(i+1, value);
	    	}
		}
		return preparedStatement;
	}
	
	// execute the preparedStatement for the insert queries, that implements rollback.
	private static Object executeNonSelect(Connection connection, String query, Object[] values) throws CustomException {
	    try (PreparedStatement preparedStatement = getPreparedStatement(connection, query, values)) {
	        preparedStatement.execute();
	        try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
	            if (generatedKeys.next()) {
	                return generatedKeys.getObject(1);
	            }
	        }
	        return null;
	    } catch (Exception e) {
	        e.printStackTrace();
	        try {
	            connection.rollback();  // Roll back if any insert fails
	        } catch (SQLException rollbackException) {
	            rollbackException.printStackTrace();
	            throw new CustomException("Rollback failed: " + rollbackException.getMessage());
	        }
	        throw new CustomException(e.getMessage());
	    } 
	}
	
	// execute the preparedStatement for the nonSelect queries.
	private static Object executeNonSelect(String query, Object[] values) throws CustomException {
		try(Connection connection = DBConnection.getConnection();
			PreparedStatement preparedStatement = getPreparedStatement(connection, query, values)) {
			preparedStatement.execute();
			try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
	            if (generatedKeys.next()) {
	                return generatedKeys.getObject(1);
	            }
	        }
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(e.getMessage());
		} 
	}
	
	// @AppendQuery method
	// sql : sql string to be appended.
	// conditions : It contains the criteria that has to be included in that query (WHERE clause).
	// conditionValues : It contains the values of the placeholder(?) in the query.
	public static <T> void QueryBuilder(StringBuilder sql,Criteria condition, List<Object> conditionValues) throws CustomException {
		List<String> joinColumn = condition.getJoinColumn(), joinValue = condition.getJoinValue(), joinOperator = condition.getJoinOperator();
	    List<Object> joinTable = condition.getJoinTable();
	    if(condition.getJoinColumn() != null) {
	    	int len = condition.getJoinColumn().size();
			for(int i=0;i<len;i++) {
				sql.append(" JOIN " + joinTable.get(i) + " ON ")
	            	.append(joinColumn.get(i))
	            	.append(" "+ joinOperator.get(i) + " ")
	            	.append(joinValue.get(i));
			}
	    }
        List<String> columns = condition.getColumn(), operators = condition.getOperator();
        List<Object> columnvalues = condition.getValue();
        if(columns.size() != 0) {
        	sql.append(" WHERE ");
        }
        for(int i=0;i<columns.size();i++) {
        	String column = columns.get(i), operator = operators.get(i);
        	Object value = columnvalues.get(i);
        	sql.append(column).append(" "); 
        	switch (operator.toUpperCase()) {
	            case "IN":
	                sql.append("IN (");
	                for (int j = 0; j < condition.getValues().size(); j++) {
	                    sql.append("?");
	                    conditionValues.add(condition.getValues().get(j));
	                    if (j < condition.getValues().size() - 1) {
	                        sql.append(", ");
	                    }
	                }
	                sql.append(")");
	                break;
	            case "BETWEEN":
	                if (condition.getValues().size() == 2) {
	                    sql.append("BETWEEN ? AND ?");
	                    conditionValues.addAll(condition.getValues());
	                }
	                break;
	            case "LIKE":
	                sql.append("LIKE ?");
	                conditionValues.add(value);
	                break;
	            case "NOT":
	                sql.append("NOT ");
	                break;
	            case "=":
	            case ">":
	            case "<":
	            case ">=":
	            case "<=":
	            	if(value.getClass() == String.class && ((String)value).contains("SELECT")) {
	            		sql.append(operator).append(" " + value);
	            		break;
	            	}
	                sql.append(operator).append(" ?");
	                conditionValues.add(value);
	                break;
	            default:
	                throw new IllegalArgumentException("Unsupported operator: " + condition.getOperator());
	        }
        	if(condition.getLogicalOperator() != null && i<columns.size()-1) {
        		sql.append(" ").append(condition.getLogicalOperator()).append(" ");
        	}
        }
        if (condition.getOrderBy() != null) {
            sql.append("  ORDER BY ").append(condition.getOrderByField()).append(" " + condition.getOrderBy());
        } if (condition.getLimitValue() != null) {
            sql.append(" LIMIT ").append(condition.getLimitValue());
        }
	}
	
	// @Update method
	// table : name of the table.
	// columnCriteriaList : it contains the column and value to be updated.
	// conditions : It contains the criteria that has to be included in that query (WHERE clause).
	@SuppressWarnings("unchecked")
	public static <T> void update(ColumnCriteria columnCriteriaList, Criteria criterias) throws CustomException {
	    Helper.checkNullValues(criterias);
	    Helper.checkNullValues(columnCriteriaList);
	    Class<? extends MarkedClass> clazz = (Class<? extends MarkedClass>) criterias.getClazz();
	    ClassMapping classMapping = ColumnYamlUtil.getMapping(criterias.getClazz().getName());
	    Map<String, FieldMapping> fieldMap = classMapping.getFields();
    	String table = classMapping.getTableName();
	    StringBuilder updateSql = new StringBuilder("UPDATE " + table + " SET ");
	    List<Object> values = new ArrayList<>(), setValues = columnCriteriaList.getValues(); 
	    List<String> setColumns = columnCriteriaList.getFields();
	    if (setColumns.size() == 0) {
	        throw new IllegalArgumentException("No columns to update.");
	    }
	    int len = updateSql.length();
	    for (int i = 0; i < setColumns.size(); i++) {
	    	String setColumn = setColumns.get(i);
	    	Object setValue = setValues.get(i);
	        FieldMapping fieldMapping = fieldMap.get(setColumn);
	        if(fieldMapping != null) {
	        	if(len < updateSql.length()) {
	        		updateSql.append(", ");
			        len = updateSql.length();
	        	}
	        	updateSql.append(fieldMapping.getColumnName()).append(" = ?");
		        values.add(setValue);
	        }
	    }
	    if(values.size() > 0) {
		    if(classMapping.getReferedField() != null) {
		    	criterias.setColumn(Arrays.asList(classMapping.getReferedField()));
		    }
	        QueryBuilder(updateSql, criterias, values);
		    executeNonSelect(updateSql.toString(), values.toArray());
	    }
	    Class<? extends MarkedClass> superclass = (Class<MarkedClass>) clazz.getSuperclass();
        if (superclass != null && !superclass.getName().equals("dblayer.model.MarkedClass")) {
        	criterias.setClazz(superclass);
        	update(columnCriteriaList, criterias);
        }   
	}

	// @Delete method
	// table : name of the table.
	// conditions : It contains the criteria that has to be included in that query (WHERE clause).
	public static <T> void delete(Criteria conditions) throws CustomException {
	    Helper.checkNullValues(conditions);
	    ClassMapping classMapping = ColumnYamlUtil.getMapping(conditions.getClazz().getName());
    	String table = classMapping.getTableName();
	    StringBuilder deleteSql = new StringBuilder("DELETE FROM ").append(table);
	    List<Object> values = new ArrayList<>();
	    QueryBuilder(deleteSql, conditions, values);
	    executeNonSelect(deleteSql.toString(), values.toArray());
	}
	
	// @Get method
	// table : name of the table.
	// clazz : class of the pojo to be returned.
	// columnCriteriaList : it contains the column and value to be updated.
	// conditions : It contains the criteria that has to be included in that query (WHERE clause).
	@SuppressWarnings("unchecked")
	public static <T> List<T> get(Criteria condition) throws CustomException {
        Helper.checkNullValues(condition);
        ClassMapping classMapping = ColumnYamlUtil.getMapping(condition.getClazz().getName());
    	String table = classMapping.getTableName();
        List<String> selectColumns = condition.getSelectColumn();
        StringBuilder selectSql = new StringBuilder("SELECT ");
        for (int i = 0; i < selectColumns.size(); i++) {
	        selectSql.append(selectColumns.get(i));
	        if (i < selectColumns.size() - 1) {
	        	selectSql.append(", ");
	        }
	    }
        selectSql.append(" FROM ").append(table);
        List<Object> conditionValues = new ArrayList<>();
        if (condition.getColumn() != null) {
            QueryBuilder(selectSql, condition, conditionValues);
        }
        List<T> list = new ArrayList<>();
        System.out.println(selectSql.toString());
        System.out.println(conditionValues);
        try (Connection connection = DBConnection.getConnection();
        	PreparedStatement preparedStatement = getPreparedStatement(connection, selectSql.toString(), conditionValues.toArray());
        	ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                list.add((T) mapResultSetToObject(resultSet, condition.getClazz(), null, table));
            }
            return list;
        } catch (SQLException e) {
            throw new CustomException("Error fetching data..");
        } 
    }
	
	// @Insert method
	// table : name of the table.
	// pojo : pojo class
	public static Object insert(Object pojo) throws CustomException {
	    Helper.checkNullValues(pojo);
	    
	    // Get the connection and begin a transaction
	    try (Connection connection = DBConnection.getConnection()) {
	        connection.setAutoCommit(false);  // Start transaction

	        Class<?> clazz = pojo.getClass();
	        List<Class<?>> classList = new ArrayList<>();
	        while(!clazz.getName().equals("dblayer.model.MarkedClass")) {
	            classList.add(clazz);
	            clazz = clazz.getSuperclass();
	        }
	        
	        Object generatedValue = null;
	        for(int k = classList.size() - 1; k >= 0; k--) {
	            clazz = classList.get(k);
	            ClassMapping classMapping = ColumnYamlUtil.getMapping(clazz.getName());
	            Map<String, FieldMapping> fieldMap = classMapping.getFields();
	            String tableName = classMapping.getTableName();

	            Field[] fields = clazz.getDeclaredFields();
	            int length = fields.length, ctr = 0;
	            StringBuilder insertSql = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
	            List<Object> values = new ArrayList<>();
	            
	            for(int i = 0; i < length; i++) {
	                Field field = fields[i];
	                if (classMapping.getAutoIncrementField() != null && classMapping.getAutoIncrementField().equals(field.getName())) {
	                    continue;
	                }
	                field.setAccessible(true); 
	                try {
	                    FieldMapping fieldMapping = fieldMap.get(field.getName());
	                    if (fieldMapping == null) {
	                        continue;
	                    }
	                    insertSql.append(fieldMapping.getColumnName()).append(", ");
	                    if (generatedValue != null && classMapping.getReferenceField() != null && classMapping.getReferenceField().equals(field.getName())) {
	                        values.add(generatedValue);
	                    } else {
	                        values.add(field.get(pojo));
	                    }
	                } catch (IllegalAccessException e) {
	                    throw new CustomException("Error accessing field value: " + e.getMessage());
	                }
	                ctr++;
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
	            Object incrementValue = executeNonSelect(connection, insertSql.toString(), values.toArray());
	            if (k == classList.size() - 1) {
	                generatedValue = incrementValue;
	            }
	        }  
	        connection.commit();
	        return generatedValue;
	    } catch (Exception e) {
	        e.printStackTrace();
	        throw new CustomException("Transaction failed and rolled back: ");
	    } 
	}
}