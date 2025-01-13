package dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import model.ColumnCriteria;
import model.Criteria;
import model.MarkedClass;

import util.CustomException;

public class DAOHelper {

	public static void addCondition(Criteria criteria, boolean condition, String column, String operator,
			Object value) {
		if (condition) {
			criteria.getColumn().add(column);
			criteria.getOperator().add(operator);
			criteria.getValue().add(value);
		}
		if (criteria.getColumn().size() > 0) {
			criteria.setLogicalOperator("AND");
		}
	}

	public static void addConditionIfPresent(Criteria criteria, Map<String, Object> map, String mapKey, String column,
			String operator, Object defaultValue) {
		Object value = map.getOrDefault(mapKey, defaultValue);
		if (value != defaultValue) {
			addCondition(criteria, true, column, operator, value);
		}
		if (criteria.getColumn().size() > 0) {
			criteria.setLogicalOperator("AND");
		}
	}

	public static void appendInClause(StringBuilder sql, Criteria condition, List<Object> conditionValues)
			throws CustomException {
		List<Object> inValues = condition.getValues();
		if (inValues == null || inValues.isEmpty()) {
			throw new CustomException("IN clause requires at least one value.");
		}

		sql.append("IN (");
		for (int j = 0; j < inValues.size(); j++) {
			sql.append("?");
			conditionValues.add(inValues.get(j));
			if (j < inValues.size() - 1) {
				sql.append(", ");
			}
		}
		sql.append(")");
	}

	// Helper method for BETWEEN operator
	public static void appendBetweenClause(StringBuilder sql, Criteria condition, List<Object> conditionValues)
			throws CustomException {
		List<Object> betweenValues = condition.getValues();
		if (betweenValues == null || betweenValues.size() != 2) {
			throw new CustomException("BETWEEN operator requires exactly two values.");
		}

		sql.append("BETWEEN ? AND ?");
		conditionValues.addAll(betweenValues);
	}

	// Helper method for comparison operators (=, <>, >, <, >=, <=)
	public static void appendComparisonOperator(StringBuilder sql, String operator, Object value,
			List<Object> conditionValues) {
		System.out.println(value);
		if (value.getClass() == String.class && ((String) value).contains("SELECT")) {
			sql.append(operator).append(" ").append(value);
		} else {
			sql.append(operator).append(" ?");
			conditionValues.add(value);
		}
	}

	public static Criteria createCriteria(Class<? extends MarkedClass> clazz, String column, String operator,
			Object value) {
		Criteria criteria = new Criteria().setClazz(clazz);
		if (column != null && operator != null && value != null) {
			criteria.getColumn().add(column);
			criteria.getOperator().add(operator);
			criteria.getValue().add(value);
		}
		return criteria;
	}

	public static ColumnCriteria createColumnCriteria(List<String> fields, List<Object> values) {
		ColumnCriteria columnCriteria = new ColumnCriteria().setFields(fields).setValues(values);
		return columnCriteria;
	}

	public static <T> Criteria initializeCriteria(Class<T> clazz) {
		Criteria criteria = new Criteria();
		criteria.setClazz(clazz);
		criteria.setSelectColumn(new ArrayList<>(Arrays.asList("*")));
		return criteria;
	}

	public static Criteria buildCriteria(Class<? extends MarkedClass> clazz, List<String> columns,
			List<String> operators, List<Object> values) {
		Criteria criteria = new Criteria();
		criteria.setClazz(clazz);
		criteria.setColumn(columns);
		criteria.setOperator(operators);
		criteria.setValue(values);
		return criteria;
	}

	public static Criteria buildJoinCriteria(Class<? extends MarkedClass> clazz, List<String> joinTable,
			List<String> joinColumn, List<String> joinOperator, List<Object> joinValue, List<String> columns,
			List<String> operators, List<Object> values) {
		Criteria criteria = buildCriteria(clazz, columns, operators, values);
		criteria.setJoinTable(joinTable);
		criteria.setJoinColumn(joinColumn);
		criteria.setJoinOperator(joinOperator);
		criteria.setJoinValue(joinValue);
		criteria.setSelectColumn(Arrays.asList("*"));
		return criteria;
	}

	public static Criteria buildJoinCriteria(Class<? extends MarkedClass> clazz, List<String> joinTable,
			List<String> joinColumn, List<String> joinOperator, List<Object> joinValue, List<String> columns,
			List<String> operators, List<Object> values, String join, boolean joinCondition) {
		Criteria criteria = buildCriteria(clazz, columns, operators, values);

		if (joinCondition) {
			criteria.setJoin(join).setJoinTable(joinTable).setJoinColumn(joinColumn).setJoinOperator(joinOperator)
					.setJoinValue(joinValue);
		}
		return criteria;
	}

	public static void addJoinCondition(Criteria criteria, boolean condition, String joinColumn, String joinOperator,
			Object joinValue) {
		if (condition) {
			criteria.getJoinColumn().add(joinColumn);
			criteria.getJoinOperator().add(joinOperator);
			criteria.getJoinValue().add(joinValue);
		}
	}

	public static void applyAccountNumberFilter(Criteria criteria, Map<String, Object> map) {
		Long accountNumber = (Long) map.get("accountNumber");
		if (accountNumber != null && accountNumber > 0) {
			if (accountNumber <= 9999) {
				addCondition(criteria, true, "RIGHT(account_number, 4)", "EQUAL_TO", accountNumber);
			} else {
				addConditionIfPresent(criteria, map, "accountNumber", "account_number", "EQUAL_TO", 0L);
			}
		}
	}

	public static String parseDuplicateEntryMessage(String errorMessage) {
		String duplicatePattern = "Duplicate entry '(.+)' for key '(.+)'";
		Pattern pattern = Pattern.compile(duplicatePattern);
		Matcher matcher = pattern.matcher(errorMessage);
		if (matcher.find()) {
			String value = matcher.group(1);
			return String.format("The value '%s' already exists. Please use a different value.", value);
		}
		return "Duplicate entry error occurred.";
	}
}
