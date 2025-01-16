package dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Enum.Constants.HttpStatusCodes;
import model.Account;
import model.ColumnCriteria;
import model.Criteria;
import model.CustomerDetail;
import model.MarkedClass;
import model.Staff;
import model.Transaction;
import model.User;
import util.CustomException;

public class DAOHelper {

	public static ColumnCriteria createColumnCriteria(List<String> fields, List<Object> values) {
		ColumnCriteria columnCriteria = new ColumnCriteria().setFields(fields).setValues(values);
		return columnCriteria;
	}

	public static <T> Criteria initializeCriteria(Class<T> clazz) {
		Criteria criteria = new Criteria().setClazz(clazz).setSelectColumn(new ArrayList<>(Arrays.asList("*")));
		return criteria;
	}

	public static Criteria buildCriteria(Class<? extends MarkedClass> clazz, List<String> columns,
			List<String> operators, List<Object> values) {
		Criteria criteria = new Criteria().setClazz(clazz).setColumn(columns).setOperator(operators).setValue(values);
		return criteria;
	}

	public static Criteria buildJoinCriteria(Class<? extends MarkedClass> clazz, List<String> joinTable,
			List<String> joinColumn, List<String> joinOperator, List<Object> joinValue, List<String> columns,
			List<String> operators, List<Object> values) {
		Criteria criteria = buildCriteria(clazz, columns, operators, values).setJoinTable(joinTable)
				.setJoinColumn(joinColumn).setJoinOperator(joinOperator).setJoinValue(joinValue)
				.setSelectColumn(Arrays.asList("*"));
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
			throw new CustomException("IN clause requires at least one value.", HttpStatusCodes.BAD_REQUEST);
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

	public static void appendBetweenClause(StringBuilder sql, Criteria condition, List<Object> conditionValues)
			throws CustomException {
		List<Object> betweenValues = condition.getValues();
		if (betweenValues == null || betweenValues.size() != 2) {
			throw new CustomException("BETWEEN operator requires exactly two values.", HttpStatusCodes.BAD_REQUEST);
		}

		sql.append("BETWEEN ? AND ?");
		conditionValues.addAll(betweenValues);
	}

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

	public static void applyAccountNumberFilter(Criteria criteria, Map<String, Object> map) {
		Long accountNumber = (Long) map.getOrDefault("accountNumber", -1l);
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
			return String.format("The value '%s' already exists.", value);
		}
		return "Duplicate entry error occurred.";
	}

	public static void applyPagination(Criteria criteria, Map<String, Object> txMap) {
		Long limitValue = (Long) txMap.getOrDefault("limit", 0L);
		Long offset = (Long) txMap.getOrDefault("offset", -1L);

		if (limitValue > 0) {
			criteria.setLimitValue(limitValue);
		}
		if (offset >= 0) {
			criteria.setOffsetValue(offset == 0 ? -1L : offset);
		}
	}

	public static Criteria applyAccountFilterBranch(Criteria criteria, Map<String, Object> accountMap) {
		if (!accountMap.containsKey("branchId")) {
			return criteria;
		}
		criteria = DAOHelper.buildJoinCriteria(Account.class, Arrays.asList("branch"), new ArrayList<>(),
				new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), " JOIN ",
				true);
		criteria.setSelectColumn(Collections.singletonList("account.*"));
		DAOHelper.addJoinCondition(criteria, true, "account.branch_id", "EQUAL_TO", "branch.id");
		return criteria;
	}

	public static Criteria applyUserFilterBranch(Criteria criteria, Map<String, Object> userMap) {
		if (!userMap.containsKey("branchId")) {
			return criteria;
		}
		List<String> joinTable = new ArrayList<>(Arrays.asList("account", "customer", "customerDetail", "staff"));
		criteria = DAOHelper
				.buildJoinCriteria(User.class, joinTable, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
						new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), " LEFT JOIN ", true)
				.setSelectColumn(Collections.singletonList("user.*"));
		DAOHelper.addJoinCondition(criteria, true, "account.user_id", "EQUAL_TO", "user.id");
		DAOHelper.addConditionIfPresent(criteria, userMap, "branchId", "account.branch_id", "EQUAL_TO", 0L);
		return criteria;
	}

	public static Criteria applyTransactionFilterBranch(Criteria criteria, Map<String, Object> txMap) {
		if (!txMap.containsKey("branchId")) {
			return criteria;
		}
		criteria = DAOHelper.buildJoinCriteria(Transaction.class, Arrays.asList("branch"), new ArrayList<>(),
				new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), " JOIN ",
				true);
		criteria.setSelectColumn(Collections.singletonList("transaction.*"));
		DAOHelper.addJoinCondition(criteria, true, "transaction.ifsc", "EQUAL_TO", "branch.ifsc_code");
		DAOHelper.addCondition(criteria, true, "branch.id", "EQUAL_TO", txMap.get("branchId"));
		return criteria;
	}

	public static void applyTransactionFilters(Criteria criteria, Map<String, Object> txMap) {
		DAOHelper.addConditionIfPresent(criteria, txMap, "customerId", "customer_id", "EQUAL_TO", 0L);
		DAOHelper.addConditionIfPresent(criteria, txMap, "from", "transaction_time", "GREATER_THAN", 0L);
		DAOHelper.addConditionIfPresent(criteria, txMap, "to", "transaction_time", "LESS_THAN", 0L);
		DAOHelper.addCondition(criteria, txMap.get("transactionType") != null, "transaction_type", "EQUAL_TO",
				txMap.get("transactionType"));
	}

	public static void applyAccountFilters(Criteria criteria, Map<String, Object> accountMap) {
		DAOHelper.addConditionIfPresent(criteria, accountMap, "userId", "user_id", "EQUAL_TO", 0L);
		DAOHelper.addConditionIfPresent(criteria, accountMap, "branchId", "branch_id", "EQUAL_TO", 0L);
		DAOHelper.addConditionIfPresent(criteria, accountMap, "accountCreated", "created_at", "EQUAL_TO", 0L);
		DAOHelper.addCondition(criteria, accountMap.get("accountType") != null, "account_type", "EQUAL_TO",
				accountMap.get("accountType"));
		DAOHelper.addCondition(criteria, accountMap.get("status") != null, "status", "EQUAL_TO",
				accountMap.get("status"));
		DAOHelper.applyAccountNumberFilter(criteria, accountMap);
	}

	public static void applyUserFilters(Criteria criteria, Map<String, Object> userMap) {
		DAOHelper.addConditionIfPresent(criteria, userMap, "userId", "user.id", "EQUAL_TO", 0L);
		DAOHelper.addConditionIfPresent(criteria, userMap, "username", "user.username", "EQUAL_TO", "");
		DAOHelper.addConditionIfPresent(criteria, userMap, "role", "user.role", "EQUAL_TO", 0L);
		DAOHelper.addConditionIfPresent(criteria, userMap, "status", "user.status", "EQUAL_TO", "");
	}

	public static Criteria getAccountCriteria(Map<String, Object> accountMap) throws CustomException {
		Criteria criteria = DAOHelper.initializeCriteria(Account.class);
		criteria = DAOHelper.applyAccountFilterBranch(criteria, accountMap);
		DAOHelper.applyAccountFilters(criteria, accountMap);
		DAOHelper.applyPagination(criteria, accountMap);
		if (accountMap.containsKey("offset")) {
			criteria.setOffsetValue((Long) accountMap.get("offset"));
		}
		return criteria;
	}

	public static <T extends MarkedClass> Criteria buildUserCriteria(Map<String, Object> userMap, Class<T> clazz,
			boolean notExact) {
		Criteria criteria = new Criteria();
		if (notExact) {
			Long userId = (Long) userMap.get("userId");
			criteria.setClazz(clazz).setSelectColumn(Arrays.asList("*")).setColumn(Arrays.asList("id", "id"))
					.setOperator(Arrays.asList("EQUAL_TO", "LIKE")).setValue(Arrays.asList(userId, "%" + userId + "%"))
					.setLimitValue(5).setLogicalOperator("OR");
		} else if (userMap.containsKey("role") && userMap.containsKey("userId")) {
			String role = (String) userMap.get("role");
			Long userId = (Long) userMap.get("userId");
			if (role.equals("Customer")) {
				criteria = DAOHelper.buildJoinCriteria(CustomerDetail.class, Arrays.asList("customer", "user"),
						Arrays.asList("customerDetail.user_id", "customer.user_id"),
						Arrays.asList("EQUAL_TO", "EQUAL_TO"), Arrays.asList("customer.user_id", "user.id"),
						Arrays.asList("customerDetail.user_id"), Arrays.asList("EQUAL_TO"), Arrays.asList(userId));
				criteria.setJoin(" JOIN ");
			} else {
				criteria = DAOHelper.buildJoinCriteria(Staff.class, Arrays.asList("user"),
						Arrays.asList("staff.user_id"), Arrays.asList("EQUAL_TO"), Arrays.asList("user.id"),
						Arrays.asList("staff.user_id"), Arrays.asList("EQUAL_TO"), Arrays.asList(userId));
				criteria.setJoin(" JOIN ");
			}
		} else {
			criteria = DAOHelper
					.buildCriteria(clazz, Arrays.asList("user.username"), Arrays.asList("EQUAL_TO"),
							Arrays.asList(userMap.get("username")))
					.setSelectColumn(Arrays.asList("user.*")).setColumn(new ArrayList<>()).setValue(new ArrayList<>())
					.setOperator(new ArrayList<>());
			criteria = DAOHelper.applyUserFilterBranch(criteria, userMap);
			DAOHelper.applyUserFilters(criteria, userMap);
		}
		if (userMap.containsKey("limit")) {
			criteria.setLimitValue(userMap.get("limit"));
		}
		return criteria;
	}
}
