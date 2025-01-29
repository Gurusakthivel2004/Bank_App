package Enum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.CustomException;

public class Constants {

	public enum ValidPaths {
		GET_USER_WITH_USERID("^/User\\?userId=\\d+&notExact=true$", Arrays.asList("Customer")),
		USER_CRUD("^/User$", Arrays.asList("Customer")),
		CREATE_BRANCH("^/Branch$", Arrays.asList("Customer", "Employee")),
		GET_BRANCH_BY_ID("^/Branch\\?branchId=\\d+&notExact=true$", Arrays.asList("Customer")),
		GET_ACCOUNT_BY_ACCOUNTNUMBER("^/Account\\?accountNumber=\\d+$", Arrays.asList("Customer")),
		GET_ACCOUNT_BY_USERID("^/Account\\?userId=-?\\d+$", Arrays.asList("Customer")),
		ACCOUNT_CRUD("^/Account$", Arrays.asList("Customer")),
		GET_USER_WITH_ROLE("^/User\\?userId=\\d+&role=\\w+$", Arrays.asList("Customer")),
		GET_USERDASHBOARD("^/UserDashboard$", null), UPDATE_PROFILE("^/api/Profile$", null),
		TRANSACTION_CRUD("^/Transaction$", null), LOGOUT("^/Logout$", null),
		LOG("^/Log$", Arrays.asList("Customer", "Employee")), MESSAGE_CRUD("^/Message$", null);
		;

		private final String regexPattern;
		private final List<String> restrictedForRole;

		ValidPaths(String regexPattern, List<String> restrictedForRole) {
			this.regexPattern = regexPattern;
			this.restrictedForRole = restrictedForRole;
		}

		ValidPaths(String regexPattern) {
			this(regexPattern, null);
		}

		public String getRegexPattern() {
			return regexPattern;
		}

		public List<String> getRestrictedForRole() {
			return restrictedForRole;
		}

		public static boolean isValidPath(String path) {
			for (ValidPaths validPath : ValidPaths.values()) {
				if (path.matches(validPath.getRegexPattern())) {
					return true;
				}
			}
			return false;
		}

		public static boolean isPathRestrictedForRole(String path, String role) {
			for (ValidPaths validPath : ValidPaths.values()) {
				if (path.matches(validPath.getRegexPattern()) && validPath.getRestrictedForRole() != null
						&& validPath.getRestrictedForRole().contains(role)) {
					return true;
				}
			}
			return false;
		}
	}

	public enum ValidQueryParams {
		USERID_FILTER("userId", ""), NOT_EXACT_VALUES("notExact", "true"), BRANCH_ID_FETCH("branchId", ""),
		ACCOUNT_FILTER("accountNumber", ""), USER_ROLE_FILTER("role", ""), LIMIT("limit", ""), STATUS("status", ""),
		OFFSET("offset", "");

		private final String param;
		private final String field;

		ValidQueryParams(String param, String field) {
			this.param = param;
			this.field = field;
		}

		public String getParam() {
			return param;
		}

		public String getField() {
			return field;
		}

		public static boolean isValidParam(String key) {
			for (ValidQueryParams validParam : ValidQueryParams.values()) {
				if (validParam.getParam().equals(key)) {
					return true;
				}
			}
			return false;
		}

		public static String getValidField(String key) {
			for (ValidQueryParams validParam : ValidQueryParams.values()) {
				if (validParam.getParam().equals(key) && validParam.getField() != "") {
					return validParam.getField();
				}
			}
			return null;
		}
	}

	public enum SelectFields {
		USER("User",
				Arrays.asList("id", "email", "phone", "role", "username", "fullname", "status", "created_at",
						"modified_at", "performed_by")),
		CUSTOMER("Customer",
				combineFields(USER.fields, Arrays.asList("customer.user_id", "pan_number", "aadhar_number"))),
		CUSTOMERDETAIL("CustomerDetail",
				combineFields(CUSTOMER.fields,
						Arrays.asList("customerDetail.user_id", "dob", "father_name", "mother_name", "address",
								"marital_status"))),
		STAFF("Staff", combineFields(USER.fields, Arrays.asList("user_id", "branch_id")));

		private final String tableName;
		private final List<String> fields;

		SelectFields(String tableName, List<String> fields) {
			this.tableName = tableName;
			this.fields = fields;
		}

		public String getTableName() {
			return tableName;
		}

		public List<String> getFields() {
			return fields;
		}

		public static SelectFields fromTableName(String tableName) {
			for (SelectFields userField : SelectFields.values()) {
				if (userField.tableName.equalsIgnoreCase(tableName)) {
					return userField;
				}
			}
			throw new IllegalArgumentException("No enum constant for table name: " + tableName);
		}

		public static List<String> getSelectFields(String tableName) {
			SelectFields userField = fromTableName(tableName);
			return userField.fields;
		}

		private static List<String> combineFields(List<String> baseFields, List<String> additionalFields) {
			List<String> combinedFields = new ArrayList<>(baseFields);
			combinedFields.addAll(additionalFields);
			return combinedFields;
		}

	}

	@SuppressWarnings("serial")
	public enum RolePermission {

		ROLE_CUSTOMER("Customer", new HashMap<String, List<String>>() {
			{
				put("Account", new ArrayList<>(Arrays.asList("GET")));
				put("User", new ArrayList<>(Arrays.asList("POST")));
				put("Branch", Arrays.asList("GET"));
				put("Transaction", Arrays.asList("GET", "POST"));
				put("UserDashboard", new ArrayList<>(Arrays.asList("GET")));
				put("Logout", new ArrayList<>(Arrays.asList("DELETE")));
				put("Message", new ArrayList<>(Arrays.asList("GET", "POST")));
			}
		}, new ArrayList<>(Arrays.asList("userId", "role", "branchId"))),
		ROLE_EMPLOYEE("Employee", new HashMap<String, List<String>>() {
			{
				put("Account", new ArrayList<>(Arrays.asList("GET", "POST", "PUT")));
				put("User", new ArrayList<>(Arrays.asList("GET", "POST", "PUT")));
				put("Branch", new ArrayList<>(Arrays.asList("GET")));
				put("Transaction", new ArrayList<>(Arrays.asList("GET", "POST")));
				put("UserDashboard", new ArrayList<>(Arrays.asList("GET")));
				put("Logout", new ArrayList<>(Arrays.asList("DELETE")));
				put("Message", new ArrayList<>(Arrays.asList("GET", "POST", "PUT")));
			}
		}, new ArrayList<>()), ROLE_MANAGER("Manager", new HashMap<String, List<String>>() {
			{
				put("Account", new ArrayList<>(Arrays.asList("GET", "POST", "PUT")));
				put("User", new ArrayList<>(Arrays.asList("GET", "POST", "PUT")));
				put("Branch", new ArrayList<>(Arrays.asList("GET", "POST", "PUT")));
				put("Transaction", new ArrayList<>(Arrays.asList("GET", "POST")));
				put("UserDashboard", new ArrayList<>(Arrays.asList("GET")));
				put("Logout", new ArrayList<>(Arrays.asList("DELETE")));
				put("Log", new ArrayList<>(Arrays.asList("POST")));
				put("Message", new ArrayList<>(Arrays.asList("GET", "POST", "PUT")));
			}
		}, new ArrayList<>());

		private final String role;
		private final Map<String, List<String>> permissions;
		private final List<String> restrictedParams;
		public static Map<String, RolePermission> ROLE_MAP = new HashMap<>();

		static {
			for (RolePermission rp : RolePermission.values()) {
				ROLE_MAP.put(rp.role, rp);
			}
		}

		RolePermission(String role, Map<String, List<String>> permissions, List<String> restrictedParams) {
			this.role = role;
			this.permissions = permissions;
			this.restrictedParams = restrictedParams;
		}

		public String getRole() {
			return role;
		}

		public static List<String> getPermissions(String role, String handler) {
			RolePermission rp = ROLE_MAP.get(role);
			return (rp != null) ? rp.permissions.getOrDefault(handler, new ArrayList<String>())
					: new ArrayList<String>();
		}

		public static List<String> getRestrictedParams(String role) {
			RolePermission rp = ROLE_MAP.get(role);
			return (rp != null) ? rp.restrictedParams : new ArrayList<String>();
		}
	}

	public static enum Operators {
		LESS_THAN("<"), GREATER_THAN(">"), LESS_THAN_OR_EQUAL_TO("<="), GREATER_THAN_OR_EQUAL_TO(">="), EQUAL_TO("="),
		NOT_EQUAL_TO("!="), AND(" AND "), OR(" OR "), NOT(" NOT "), LIKE(" LIKE "), BETWEEN(" BETWEEN "), IN(" IN ");

		private final String symbol;

		Operators(String symbol) {
			this.symbol = symbol;
		}

		public String getSymbol() {
			return symbol;
		}

		public static String get(String operator) {
			try {
				return Operators.valueOf(operator).getSymbol();
			} catch (IllegalArgumentException e) {
				return operator;
			}
		}
	}

	public static enum AggregateFunction {
		AVG("AVG"), MIN("MIN"), MAX("MAX"), SUM("SUM"), COUNT("COUNT");

		private final String function;

		AggregateFunction(String function) {
			this.function = function;
		}

		public String apply(String attribute) {
			return function + "(" + attribute + ")";
		}

		public static String get(String function, String attribute) {
			try {
				return AggregateFunction.valueOf(function.toUpperCase()).apply(attribute);
			} catch (IllegalArgumentException e) {
				return "function(" + attribute + ")";
			}
		}
	}

	public enum HttpStatusCodes {
		INTERNAL_SERVER_ERROR(500, "Internal Server Error"), BAD_REQUEST(400, "Bad Request"),
		TOO_MANY_REQUESTS(429, "Too Many Requests"), NOT_FOUND(404, "Not Found"), OK(200, "OK"),
		CONFLICT(409, "Conflict"), FORBIDDEN(403, "Forbidden"), UNAUTHORIZED(401, "Unauthorized");

		private final int code;
		private final String message;

		HttpStatusCodes(int code, String message) {
			this.code = code;
			this.message = message;
		}

		public int getCode() {
			return code;
		}

		public String getMessage() {
			return message;
		}
	}

	public enum Status {
		Suspended, Active, Inactive;

		@Override
		public String toString() {
			return name();
		}

		public static Status fromString(String status) throws CustomException {
			try {
				return Status.valueOf(status);
			} catch (IllegalArgumentException e) {
				throw new CustomException("Invalid status: " + status, HttpStatusCodes.BAD_REQUEST);
			}
		}
	}

	public enum LogType {
		Insert, Update, Delete, Login, Logout;

		@Override
		public String toString() {
			return name();
		}

		public static LogType fromString(String logType) throws CustomException {
			try {
				return LogType.valueOf(logType);
			} catch (IllegalArgumentException e) {
				throw new CustomException("Invalid log type: " + logType, HttpStatusCodes.BAD_REQUEST);
			}
		}
	}

	public enum AccountType {
		Savings, Current, Fixed_Deposit, Operational;

		@Override
		public String toString() {
			return name();
		}

		public static AccountType fromString(String accountType) throws CustomException {
			try {
				return AccountType.valueOf(accountType);
			} catch (IllegalArgumentException e) {
				throw new CustomException("Invalid account type: " + accountType, HttpStatusCodes.BAD_REQUEST);
			}
		}
	}

	public enum Role {
		Customer, Employee, Manager;

		@Override
		public String toString() {
			return name();
		}

		public static Role fromString(String role) throws CustomException {
			try {
				return Role.valueOf(role);
			} catch (IllegalArgumentException e) {
				throw new CustomException("Invalid role: " + role, HttpStatusCodes.BAD_REQUEST);
			}
		}
	}

	public enum TransactionType {
		Credit, Debit, Deposit, Withdraw, Default;

		@Override
		public String toString() {
			return name();
		}

		public static TransactionType fromString(String transactionType) throws CustomException {
			try {
				return TransactionType.valueOf(transactionType);
			} catch (IllegalArgumentException e) {
				throw new CustomException("Invalid transaction type: " + transactionType, HttpStatusCodes.BAD_REQUEST);
			}
		}
	}

	public enum MessageType {
		AccountRequest, TransactionRequest, UserRequest, Chat;

		@Override
		public String toString() {
			return name();
		}

		public static MessageType fromString(String messageType) throws CustomException {
			try {
				return MessageType.valueOf(messageType);
			} catch (IllegalArgumentException e) {
				throw new CustomException("Invalid transaction type: " + messageType, HttpStatusCodes.BAD_REQUEST);
			}
		}
	}

	public enum MessageStatus {
		Completed, Pending, Expired, Cancelled;

		@Override
		public String toString() {
			return name();
		}

		public static MessageStatus fromString(String messageStatus) throws CustomException {
			try {
				return MessageStatus.valueOf(messageStatus);
			} catch (IllegalArgumentException e) {
				throw new CustomException("Invalid message status: " + messageStatus, HttpStatusCodes.BAD_REQUEST);
			}
		}
	}

	public enum TransactionStatus {
		Completed, Pending, Cancelled, Active, Expired;

		@Override
		public String toString() {
			return name();
		}

		public static TransactionStatus fromString(String transactionStatus) throws CustomException {
			try {
				return TransactionStatus.valueOf(transactionStatus);
			} catch (IllegalArgumentException e) {
				throw new CustomException("Invalid transaction status: " + transactionStatus,
						HttpStatusCodes.BAD_REQUEST);
			}
		}
	}

}