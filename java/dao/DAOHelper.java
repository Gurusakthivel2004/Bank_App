package dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import enums.Constants.HttpStatusCodes;
import enums.Constants.Role;
import enums.Constants.SelectFields;
import model.Account;
import model.ActivityLog;
import model.ColumnCriteria;
import model.Criteria;
import model.CustomerDetail;
import model.MarkedClass;
import model.Message;
import model.Staff;
import model.Transaction;
import model.User;
import util.CustomException;
import util.Helper;

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
			List<String> operators, List<Object> values, String join, boolean joinCondition) {
		Criteria criteria = buildCriteria(clazz, columns, operators, values);
		if (joinCondition) {
			criteria.setJoin(join).setJoinTable(joinTable).setJoinColumn(joinColumn).setJoinOperator(joinOperator)
					.setJoinValue(joinValue);
		} else {
			criteria.setJoinTable(joinTable).setJoinColumn(joinColumn).setJoinOperator(joinOperator)
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
		if ((mapKey.equals("from") || mapKey.equals("to")) && (value != defaultValue)) {
			value = Helper.convertDateToMillis(value.toString());
		}
		List<String> enumKeys = Arrays.asList("role", "status", "accountType", "transactionType");
		if (value != defaultValue) {
			if (enumKeys.contains(mapKey)) {
				value = value.toString();
			}
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

	public static void applyAccountNumberFilter(Criteria criteria, Map<String, Object> map, String table) {
		Long accountNumber = (Long) map.getOrDefault("accountNumber", -1l);
		if (accountNumber != null && accountNumber > 0) {
			if (accountNumber <= 9999) {
				addCondition(criteria, true, "RIGHT(" + table + ".account_number" + ", 4)", "EQUAL_TO", accountNumber);
			} else {
				addConditionIfPresent(criteria, map, "accountNumber", table + ".account_number", "EQUAL_TO", 0L);
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

	public static void applyPagination(Criteria criteria, Map<String, Object> map) {
		Long limitValue = (Long) map.getOrDefault("limit", 0L);
		Long offset = (Long) map.getOrDefault("offset", -1L);

		if (limitValue > 0) {
			criteria.setLimitValue(limitValue);
		}
		if (offset >= 0) {
			criteria.setOffsetValue(offset);
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

	public static Criteria applyMessageFilterBranch(Criteria criteria, Map<String, Object> userMap) {
		if (!userMap.containsKey("branchId")) {
			return criteria;
		}
		List<String> joinTable = new ArrayList<>(Arrays.asList("account", "branch"));
		criteria = DAOHelper
				.buildJoinCriteria(Message.class, joinTable, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
						new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), " JOIN ", true)
				.setSelectColumn(Collections.singletonList("*"));
		DAOHelper.addJoinCondition(criteria, true, "account.user_id", "EQUAL_TO", "message.sender_id");
		DAOHelper.addJoinCondition(criteria, true, "branch.id", "EQUAL_TO", "account.branch_id");
		DAOHelper.addConditionIfPresent(criteria, userMap, "branchId", "account.branch_id", "EQUAL_TO", 0L);
		DAOHelper.addConditionIfPresent(criteria, userMap, "status", "message_status", "EQUAL_TO", "");
		return criteria;
	}

	public static Criteria applyTransactionFilterBranch(Criteria criteria, Map<String, Object> txMap) {
		if (!txMap.containsKey("branchId")) {
			return criteria;
		}
		List<String> joinTable = new ArrayList<>(Arrays.asList("account", "branch"));
		criteria = DAOHelper
				.buildJoinCriteria(Transaction.class, joinTable, new ArrayList<>(), new ArrayList<>(),
						new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), " JOIN ", true)
				.setSelectColumn(Collections.singletonList("*"));
		DAOHelper.addJoinCondition(criteria, true, "account.account_number", "EQUAL_TO", "transaction.account_number");
		DAOHelper.addJoinCondition(criteria, true, "branch.id", "EQUAL_TO", "account.branch_id");
		DAOHelper.addCondition(criteria, true, "branch.id", "EQUAL_TO", txMap.get("branchId"));
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

	public static void applyTransactionFilters(Criteria criteria, Map<String, Object> txMap) {
		DAOHelper.addConditionIfPresent(criteria, txMap, "customerId", "customer_id", "EQUAL_TO", 0L);
		DAOHelper.addConditionIfPresent(criteria, txMap, "from", "transaction_time", "GREATER_THAN", 0L);
		DAOHelper.addConditionIfPresent(criteria, txMap, "to", "transaction_time", "LESS_THAN", 0L);
		DAOHelper.addConditionIfPresent(criteria, txMap, "transactionType", "transaction_type", "EQUAL_TO", "");
	}

	public static void applyLogFilters(Criteria criteria, Map<String, Object> logMap) {
		DAOHelper.addConditionIfPresent(criteria, logMap, "userId", "user_id", "EQUAL_TO", 0L);
		DAOHelper.addConditionIfPresent(criteria, logMap, "userAccountNumber", "user_account_number", "EQUAL_TO", 0L);
		DAOHelper.addConditionIfPresent(criteria, logMap, "performedBy", "performed_by", "EQUAL_TO", 0L);
		DAOHelper.addConditionIfPresent(criteria, logMap, "logType", "log_type", "EQUAL_TO", 0L);
		DAOHelper.addConditionIfPresent(criteria, logMap, "from", "timestamp", "GREATER_THAN", 0L);
		DAOHelper.addConditionIfPresent(criteria, logMap, "to", "timestamp", "LESS_THAN", 0L);
	}

	public static void applyAccountFilters(Criteria criteria, Map<String, Object> accountMap) {
		DAOHelper.addConditionIfPresent(criteria, accountMap, "userId", "user_id", "EQUAL_TO", 0L);
		DAOHelper.addConditionIfPresent(criteria, accountMap, "branchId", "branch_id", "EQUAL_TO", 0L);
		DAOHelper.addConditionIfPresent(criteria, accountMap, "accountCreated", "created_at", "EQUAL_TO", 0L);
		DAOHelper.addConditionIfPresent(criteria, accountMap, "accountType", "account_type", "EQUAL_TO", "");
		DAOHelper.addConditionIfPresent(criteria, accountMap, "status", "status", "EQUAL_TO", "");
		DAOHelper.applyAccountNumberFilter(criteria, accountMap, "account");
	}

	public static <T extends User> void applyUserFilters(Criteria criteria, Map<String, Object> userMap,
			Class<T> clazz) {
		String idColumn = clazz == User.class ? "user.id" : "user_id";
		DAOHelper.addConditionIfPresent(criteria, userMap, "userId", idColumn, "EQUAL_TO", 0L);
		DAOHelper.addConditionIfPresent(criteria, userMap, "username", "user.username", "EQUAL_TO", "");
		DAOHelper.addConditionIfPresent(criteria, userMap, "role", "user.role", "EQUAL_TO", "");
		DAOHelper.addConditionIfPresent(criteria, userMap, "email", "user.email", "EQUAL_TO", "");
		DAOHelper.addConditionIfPresent(criteria, userMap, "status", "user.status", "EQUAL_TO", "");
	}

	public static Criteria getAccountCriteria(Map<String, Object> accountMap) throws CustomException {
		Criteria criteria = DAOHelper.initializeCriteria(Account.class);
		criteria = DAOHelper.applyAccountFilterBranch(criteria, accountMap);
		DAOHelper.applyAccountFilters(criteria, accountMap);
		DAOHelper.applyPagination(criteria, accountMap);
		return criteria;
	}

	public static Criteria getLogCriteria(Map<String, Object> logMap) throws CustomException {
		Criteria criteria = DAOHelper.initializeCriteria(ActivityLog.class);
		DAOHelper.applyLogFilters(criteria, logMap);
		DAOHelper.applyPagination(criteria, logMap);
		DAOHelper.applyAccountNumberFilter(criteria, logMap, "activityLog");
		criteria.setOrderByField("timestamp").setOrderBy("DESC");
		return criteria;
	}

	public static Criteria getMessageCriteria(Map<String, Object> messageMap) throws CustomException {
		Criteria criteria = DAOHelper.initializeCriteria(Message.class);
		criteria = DAOHelper.applyMessageFilterBranch(criteria, messageMap);
		DAOHelper.applyPagination(criteria, messageMap);
		criteria.setSelectColumn(new ArrayList<>(Arrays.asList("DISTINCT message.*")));
		return criteria;
	}

	public static <T extends User> Criteria buildUserCriteria(Map<String, Object> userMap, Class<T> clazz,
			boolean notExact) throws CustomException {
		Criteria criteria = new Criteria();
		if (notExact) {
			Long userId = (Long) userMap.get("userId");
			criteria.setClazz(clazz).setSelectColumn(SelectFields.getSelectFields(clazz.getSimpleName()))
					.setColumn(Arrays.asList("id", "id")).setOperator(Arrays.asList("EQUAL_TO", "LIKE"))
					.setValue(Arrays.asList(userId, "%" + userId + "%")).setLimitValue(5).setLogicalOperator("OR");
		} else if (userMap.containsKey("role") && userMap.containsKey("userId")) {
			Role role = (Role) userMap.get("role");
			Long userId = (Long) userMap.get("userId");
			if (role == Role.Customer) {
				criteria = DAOHelper.buildJoinCriteria(CustomerDetail.class, Arrays.asList("customer", "user"),
						Arrays.asList("customerDetail.user_id", "customer.user_id"),
						Arrays.asList("EQUAL_TO", "EQUAL_TO"), Arrays.asList("customer.user_id", "user.id"),
						Arrays.asList("customerDetail.user_id"), Arrays.asList("EQUAL_TO"), Arrays.asList(userId), null,
						false);
				criteria.setJoin(" JOIN ").setSelectColumn(SelectFields.getSelectFields(clazz.getSimpleName()));
			} else {
				criteria = DAOHelper.buildJoinCriteria(Staff.class, Arrays.asList("user"),
						Arrays.asList("staff.user_id"), Arrays.asList("EQUAL_TO"), Arrays.asList("user.id"),
						Arrays.asList("staff.user_id"), Arrays.asList("EQUAL_TO"), Arrays.asList(userId), null, false);
				criteria.setJoin(" JOIN ").setSelectColumn(SelectFields.getSelectFields(clazz.getSimpleName()));
			}
		} else if (userMap.containsKey("username")) {
			criteria = DAOHelper.buildCriteria(clazz, Arrays.asList("user.username"), Arrays.asList("EQUAL_TO"),
					Arrays.asList(userMap.get("username"))).setSelectColumn(Arrays.asList("user.*"));
//			criteria = DAOHelper.applyUserFilterBranch(criteria, userMap);
		} else {
			criteria.setSelectColumn(SelectFields.getSelectFields(clazz.getSimpleName())).setClazz(clazz);
			criteria = DAOHelper.applyUserFilterBranch(criteria, userMap);
			applyUserFilters(criteria, userMap, clazz);
		}
		if (userMap.containsKey("limit")) {
			criteria.setLimitValue(userMap.get("limit"));
		}
		if (userMap.containsKey("offset")) {
			criteria.setOffsetValue((Long) userMap.get("offset"));
		}
		if (userMap.containsKey("password")) {
			criteria.setSelectColumn(Arrays.asList("*"));
		}
		return criteria;
	}
}