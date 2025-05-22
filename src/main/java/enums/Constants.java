package enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
		GET_USER_WITH_ROLE("^/User\\?userId=\\d+&ROLE=\\w+$", Arrays.asList("Customer")),
		GET_USERDASHBOARD("^/UserDashboard$"), TRANSACTION_CRUD("^/Transaction$"), LOGOUT("^/Logout$"),
		LOG("^/Log$", Arrays.asList("Customer", "Employee")), MESSAGE_CRUD("^/Message$"), OAUTH("^/oauth"),
		OAUTHCALLBACK("^/oauthCallback"), CAPTCHA("^/Captcha"), FIXED_DEPOSIT("^/FixedDeposit"), ORG("^/Org"),
		SUB_ORG("^/SubOrg"), CREATE_OTP("^/CreateOtp");

		private final String REGEX_PATTERN;
		private final List<String> restrictedForRole;

		ValidPaths(String REGEX_PATTERN, List<String> restrictedForRole) {
			this.REGEX_PATTERN = REGEX_PATTERN;
			this.restrictedForRole = restrictedForRole;
		}

		ValidPaths(String REGEX_PATTERN) {
			this(REGEX_PATTERN, null);
		}

		public String getRegexPattern() {
			return REGEX_PATTERN;
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

		public static boolean isPathRestrictedForRole(String path, String ROLE) {
			for (ValidPaths validPath : ValidPaths.values()) {
				if (path.matches(validPath.getRegexPattern()) && validPath.getRestrictedForRole() != null
						&& validPath.getRestrictedForRole().contains(ROLE)) {
					return true;
				}
			}
			return false;
		}
	}

	public enum ValidQueryParams {
		USERID_FILTER("userId", ""), NOT_EXACT_VALUES("notExact", "true"), BRANCH_ID_FETCH("branchId", ""),
		ACCOUNT_FILTER("accountNumber", ""), USER_ROLE_FILTER("role", ""), LIMIT("limit", ""), STATUS("status", ""),
		OFFSET("offset", ""), NAME("name", ""), Org_Id("orgId", "");

		private final String PARAM;
		private final String FIELD;

		ValidQueryParams(String PARAM, String FIELD) {
			this.PARAM = PARAM;
			this.FIELD = FIELD;
		}

		public String getParam() {
			return PARAM;
		}

		public String getField() {
			return FIELD;
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

	@SuppressWarnings("serial")
	public enum RolePermission {

		ROLE_CUSTOMER("Customer", new HashMap<String, List<String>>() {
			{
				put("Account", new ArrayList<>(Arrays.asList("GET")));
				put("User", new ArrayList<>(Arrays.asList("GET", "PUT")));
				put("Branch", Arrays.asList("GET"));
				put("Transaction", Arrays.asList("GET", "POST"));
				put("UserDashboard", new ArrayList<>(Arrays.asList("GET")));
				put("Logout", new ArrayList<>(Arrays.asList("DELETE")));
				put("Message", new ArrayList<>(Arrays.asList("GET", "POST")));
				put("Otp", new ArrayList<>(Arrays.asList("GET", "POST")));
				put("FixedDeposit", new ArrayList<>(Arrays.asList("POST")));
			}
		}, new ArrayList<>(Arrays.asList("userId", "ROLE", "branchId"))),
		ROLE_EMPLOYEE("Employee", new HashMap<String, List<String>>() {
			{
				put("Account", new ArrayList<>(Arrays.asList("GET", "POST", "PUT")));
				put("User", new ArrayList<>(Arrays.asList("GET", "POST", "PUT")));
				put("Branch", new ArrayList<>(Arrays.asList("GET")));
				put("Transaction", new ArrayList<>(Arrays.asList("GET", "POST")));
				put("UserDashboard", new ArrayList<>(Arrays.asList("GET")));
				put("Logout", new ArrayList<>(Arrays.asList("DELETE")));
				put("Message", new ArrayList<>(Arrays.asList("GET", "POST", "PUT")));
				put("Otp", new ArrayList<>(Arrays.asList("GET", "POST")));
				put("FixedDeposit", new ArrayList<>(Arrays.asList("GET", "POST")));
				put("Org", new ArrayList<>(Arrays.asList("POST", "GET")));
				put("SubOrg", new ArrayList<>(Arrays.asList("POST", "GET")));
				put("CreateOtp", new ArrayList<>(Arrays.asList("POST")));
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
				put("Otp", new ArrayList<>(Arrays.asList("GET", "POST")));
				put("FixedDeposit", new ArrayList<>(Arrays.asList("GET", "POST")));
				put("Org", new ArrayList<>(Arrays.asList("POST", "GET")));
				put("SubOrg", new ArrayList<>(Arrays.asList("POST", "GET")));
				put("CreateOtp", new ArrayList<>(Arrays.asList("POST")));
			}
		}, new ArrayList<>());

		private final String ROLE;
		private final Map<String, List<String>> permissions;
		private final List<String> RESTRICTED_PARAMS;
		public static Map<String, RolePermission> ROLE_MAP = new HashMap<>();

		static {
			for (RolePermission rp : RolePermission.values()) {
				ROLE_MAP.put(rp.ROLE, rp);
			}
		}

		RolePermission(String role, Map<String, List<String>> permissions, List<String> RESTRICTED_PARAMS) {
			this.ROLE = role;
			this.permissions = permissions;
			this.RESTRICTED_PARAMS = RESTRICTED_PARAMS;
		}

		public String getRole() {
			return ROLE;
		}

		public static List<String> getPermissions(String role, String handler) {
			RolePermission rp = ROLE_MAP.get(role);
			return (rp != null) ? rp.permissions.getOrDefault(handler, new ArrayList<String>())
					: new ArrayList<String>();
		}

		public static List<String> getRestrictedParams(String role) {
			RolePermission rp = ROLE_MAP.get(role);
			return (rp != null) ? rp.RESTRICTED_PARAMS : new ArrayList<String>();
		}
	}

	public enum SelectFields {
		USER("User",
				Arrays.asList("id", "email", "phone", "ROLE", "username", "fullname", "status", "created_at",
						"modified_at", "performed_by")),
		CUSTOMER("Customer",
				combineFields(USER.FIELDS, Arrays.asList("customer.user_id", "pan_number", "aadhar_number"))),
		CUSTOMERDETAIL("CustomerDetail",
				combineFields(CUSTOMER.FIELDS,
						Arrays.asList("customerDetail.user_id", "dob", "father_name", "mother_name", "address",
								"marital_status"))),
		STAFF("Staff", combineFields(USER.FIELDS, Arrays.asList("user_id", "branch_id")));

		private final String TABLE_NAME;
		private final List<String> FIELDS;

		SelectFields(String TABLE_NAME, List<String> FIELDS) {
			this.TABLE_NAME = TABLE_NAME;
			this.FIELDS = FIELDS;
		}

		public String getTableName() {
			return TABLE_NAME;
		}

		public List<String> getFields() {
			return FIELDS;
		}

		public static SelectFields fromTableName(String TABLE_NAME) {
			for (SelectFields userField : SelectFields.values()) {
				if (userField.TABLE_NAME.equalsIgnoreCase(TABLE_NAME)) {
					return userField;
				}
			}
			throw new IllegalArgumentException("No enum constant for table name: " + TABLE_NAME);
		}

		public static List<String> getSelectFields(String TABLE_NAME) {
			SelectFields userField = fromTableName(TABLE_NAME);
			return userField.FIELDS;
		}

		private static List<String> combineFields(List<String> baseFields, List<String> additionalFields) {
			List<String> combinedFields = new ArrayList<>(baseFields);
			combinedFields.addAll(additionalFields);
			return combinedFields;
		}

	}

	public static enum Operators {
		LESS_THAN("<"), GREATER_THAN(">"), LESS_THAN_OR_EQUAL_TO("<="), GREATER_THAN_OR_EQUAL_TO(">="), EQUAL_TO("="),
		NOT_EQUAL_TO("!="), AND(" AND "), OR(" OR "), NOT(" NOT "), LIKE(" LIKE "), BETWEEN(" BETWEEN "), IN(" IN ");

		private final String SYMBOL;

		Operators(String SYMBOL) {
			this.SYMBOL = SYMBOL;
		}

		public String getSymbol() {
			return SYMBOL;
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
		CONFLICT(409, "Conflict"), FORBIDDEN(403, "Forbidden"), UNAUTHORIZED(401, "Unauthorized"),
		INVALID_DATA(400, "Invalid Data"), INTERNAL_ERROR(500, "Internal Error"), INVALID_MODULE(400, "Invalid Module"),
		INVALID_REQUEST_METHOD(400, "Invalid Request Method");

		private final int CODE;
		private final String MESSAGE;

		HttpStatusCodes(int CODE, String MESSAGE) {
			this.CODE = CODE;
			this.MESSAGE = MESSAGE;
		}

		public int getCode() {
			return CODE;
		}

		public String getMessage() {
			return MESSAGE;
		}

		public static Optional<HttpStatusCodes> fromName(String name) {
			try {
				return Optional.of(HttpStatusCodes.valueOf(name));
			} catch (IllegalArgumentException e) {
				return Optional.empty();
			}
		}

	}

	public enum TaskExecutor {
		MAIL(5), LOG(5), CRM(5);

		private final ExecutorService executor;

		TaskExecutor(int nThreads) {
			this.executor = Executors.newFixedThreadPool(nThreads);
		}

		public void submitTask(Runnable task) {
			executor.submit(task);
		}

		public <T> Future<T> submitTask(Callable<T> task) {
			return executor.submit(task);
		}

		public void shutdown() {
			executor.shutdown();
		}

		public static void shutdownAll() {
			for (TaskExecutor exec : values()) {
				exec.shutdown();
			}
		}

	}

	public enum ModuleCode {
		Accounts(0), Contacts(1), Deals(2), Leads(3), Accountss(5);

		private final Integer id;

		private static final Map<Integer, ModuleCode> ID_TO_ENUM_MAP = new HashMap<>();

		static {
			for (ModuleCode useCase : values()) {
				ID_TO_ENUM_MAP.put(useCase.id, useCase);
			}
		}

		ModuleCode(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public static ModuleCode fromId(Integer id) {
			return ID_TO_ENUM_MAP.get(id);
		}

		public static ModuleCode fromName(String name) {
			for (ModuleCode code : values()) {
				if (code.name().equalsIgnoreCase(name)) {
					return code;
				}
			}
			return null;
		}

	}

	public enum UseCase {
		ORG_PUSH(1), DEAL_PUSH(2), PHONE_VALIDATION(3), CUSTOM_UPDATE(4);

		private final Integer id;

		private static final Map<Integer, UseCase> ID_TO_ENUM_MAP = new HashMap<>();

		static {
			for (UseCase useCase : values()) {
				ID_TO_ENUM_MAP.put(useCase.id, useCase);
			}
		}

		UseCase(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public static UseCase fromId(Integer id) {
			return ID_TO_ENUM_MAP.get(id);
		}
	}

	public interface SymbolProvider {
		String getSymbol();
	}

	public enum HttpMethod {
		GET, POST, PUT
	}

	public static enum ContactsFields implements SymbolProvider {
		FK_USER_ID("FK_User_Id"), ID("id"), EMAIL("Email"), FIRST_NAME("First_Name"), LAST_NAME("Last_Name"),
		PHONE("Phone"), DOB("Date_Of_Birth"), FK_ACCOUNT_NAME("FK_Account_Name"), IDENTIFIER("Identifier"),
		EMAIL_STATUS("Email_Status"), COUNTRY_CODE("Country_Code"), USERNAME_STATUS("Username_Status");

		private final String ApiName;

		ContactsFields(String ApiName) {
			this.ApiName = ApiName;
		}

		public String getSymbol() {
			return ApiName;
		}

		public static String name(String operator) {
			try {
				return ContactsFields.valueOf(operator).getSymbol();
			} catch (IllegalArgumentException e) {
				return operator;
			}
		}
	}

	public static enum AccountsFields implements SymbolProvider {
		FK_USER_ID("FK_User_Id"), ID("id"), PHONE("Phone"), ACCOUNT_NAME("Account_Name"), INDUSTRY("Industry"),
		EMPLOYEES("Employees"), IDENTIFIER("Identifier");
		;

		private final String ApiName;

		AccountsFields(String ApiName) {
			this.ApiName = ApiName;
		}

		public String getSymbol() {
			return ApiName;
		}

		public static String name(String operator) {
			try {
				return AccountsFields.valueOf(operator).getSymbol();
			} catch (IllegalArgumentException e) {
				return operator;
			}
		}
	}

	public static enum DealsFields implements SymbolProvider {
		USER_ID("id"), ID("id"), MODULE_RECORD_ID("Module_Record_Id"), AMOUNT("Amount"), STAGE("Stage"),
		DEAL_NAME("Deal_Name"), TYPE("Type"), FK_ACCOUNT_NAME("FK_Account_Name"), FK_CONTACT_NAME("FK_Contact_Name");

		private final String ApiName;

		DealsFields(String ApiName) {
			this.ApiName = ApiName;
		}

		public String getSymbol() {
			return ApiName;
		}

		public static String name(String operator) {
			try {
				return DealsFields.valueOf(operator).getSymbol();
			} catch (IllegalArgumentException e) {
				return operator;
			}
		}
	}

	public static enum LeadsFields implements SymbolProvider {
		COMPANY("Company"), FIRST_NAME("First_Name"), LAST_NAME("Last_Name"), EMAIL("Email");

		private final String ApiName;

		LeadsFields(String ApiName) {
			this.ApiName = ApiName;
		}

		public String getSymbol() {
			return ApiName;
		}

		public static String name(String operator) {
			try {
				return LeadsFields.valueOf(operator).getSymbol();
			} catch (IllegalArgumentException e) {
				return operator;
			}
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

	public enum Country {
		INDIA("India", "IN"), UNITED_STATES("United States", "US"), UNITED_KINGDOM("United Kingdom", "GB"),
		GERMANY("Germany", "DE"), FRANCE("France", "FR"), CANADA("Canada", "CA"), AUSTRALIA("Australia", "AU"),
		SINGAPORE("Singapore", "SG"), JAPAN("Japan", "JP"), BRAZIL("Brazil", "BR");

		private final String countryName;
		private final String regionCode;

		Country(String countryName, String regionCode) {
			this.countryName = countryName;
			this.regionCode = regionCode;
		}

		public String getCountryName() {
			return countryName;
		}

		public String getRegionCode() {
			return regionCode;
		}

		public static Country fromCountryName(String name) {
			for (Country country : values()) {
				if (country.countryName.equalsIgnoreCase(name)) {
					return country;
				}
			}
			return null;
		}

		public static Country fromRegionCode(String regionCode) {
			for (Country country : values()) {
				if (country.getRegionCode().equalsIgnoreCase(regionCode)) {
					return country;
				}
			}
			return null;
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
		Customer, Employee, Manager, Admin, User;

		@Override
		public String toString() {
			return name();
		}

		public static Role fromString(String ROLE) throws CustomException {
			try {
				return Role.valueOf(ROLE);
			} catch (IllegalArgumentException e) {
				throw new CustomException("Invalid ROLE: " + ROLE, HttpStatusCodes.BAD_REQUEST);
			}
		}
	}

	public enum TransactionType {
		Credit, Debit, Deposit, Withdraw, Loan, FixedDeposit, Default;

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
		AccountRequest, TransactionRequest, UserRequest, ApplyLoan, Chat;

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
				throw new CustomException("Invalid MESSAGE status: " + messageStatus, HttpStatusCodes.BAD_REQUEST);
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

	public enum Module {
		Loan, FixedDeposit;

		@Override
		public String toString() {
			return name();
		}

		public static Module fromString(String module) throws CustomException {
			try {
				return Module.valueOf(module);
			} catch (IllegalArgumentException e) {
				throw new CustomException("Invalid module: " + module, HttpStatusCodes.BAD_REQUEST);
			}
		}
	}

	public enum LoanAction {
		Apply, Approve, Reject;

		@Override
		public String toString() {
			return name();
		}

		public static LoanAction fromString(String loanAction) throws CustomException {
			try {
				return LoanAction.valueOf(loanAction);
			} catch (IllegalArgumentException e) {
				throw new CustomException("Invalid loan action: " + loanAction, HttpStatusCodes.BAD_REQUEST);
			}
		}
	}

}