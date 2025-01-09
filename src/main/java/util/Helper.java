package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import Enum.Constants.HttpStatusCodes;
import Enum.Constants.ValidQueryParams;
import dblayer.model.ColumnCriteria;
import dblayer.model.Criteria;
import dblayer.model.MarkedClass;
import dblayer.model.Transaction;
import service.BranchService;
import util.ColumnYamlUtil.ClassMapping;

public class Helper {

	private static final Logger logger = LogManager.getLogger(BranchService.class);
	private static ThreadLocal<Map<String, Object>> threadLocal = ThreadLocal
			.withInitial(() -> new HashMap<String, Object>());

	public static Map<String, Object> getThreadLocalValue() {
		return threadLocal.get();
	}

	public static void setThreadLocalValue(Map<String, Object> newValue) {
		threadLocal.set(newValue);
	}

	public static void clearThreadLocal() {
		threadLocal.remove();
	}

	public static String hashPassword(String password) {
		return BCrypt.hashpw(password, BCrypt.gensalt());
	}

	public static boolean checkPassword(String password, String hashed) {
		System.out.println(password + " " + hashed);
		System.out.println(BCrypt.checkpw(password, hashed));
		return BCrypt.checkpw(password, hashed);
	}

	public static void checkNullValues(Object inputObject) throws CustomException {
		if (inputObject == null) {
			throw new CustomException("Error: Null value provided.");
		}
	}

	public static void checkNull(Object obj, String message) throws CustomException {
		if (obj == null) {
			throw new CustomException(message);
		}
	}

	public static void checkEmptyString(String str, String message) throws CustomException {
		if (str == null || str.isEmpty()) {
			throw new CustomException(message);
		}
	}

	public static void validateClassMapping(Class<?> clazz, ClassMapping classMapping) throws CustomException {
		checkNull(clazz, "Class type cannot be null.");
		checkNull(classMapping, "Class mapping not found for class: " + clazz.getName());
	}

	public static void validateTableName(String table, String clazzName) throws CustomException {
		checkEmptyString(table, "Table name is not defined for class: " + clazzName);
	}

	public static void validateQueryConditions(List<?> conditions, String message) throws CustomException {
		if (conditions == null || conditions.isEmpty()) {
			throw new CustomException(message);
		}
	}

	public static void handleSQLException(SQLException e) throws CustomException {
		logger.error("SQL error: " + e.getMessage(), e);
		throw new CustomException("SQL error occurred.");
	}

	public static void handleGeneralException(Exception e, String message) throws CustomException {
		logger.error(message + ": " + e.getMessage(), e);
		throw new CustomException(message);
	}

	public static void checkNumber(String number) throws CustomException {
		checkNullValues(number);
		String patternString = "^\\d{10}$";
		if (!Pattern.matches(patternString, number)) {
			throw new CustomException("Error: Mobile number must have 10 digits!");
		}
	}

	public static void checkLength(Object[] arr1, Object[] arr2) throws CustomException {
		checkNullValues(arr1);
		checkNullValues(arr2);
		if (arr1.length != arr2.length) {
			throw new CustomException("Both arrays must have the same length.");
		}
	}

	public static void checkEmail(String email) throws CustomException {
		checkNullValues(email);
		String patternString = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9-]+\\.[a-zA-Z.]{2,18}$";
		if (!Pattern.matches(patternString, email)) {
			throw new CustomException("Error: Email is not valid");
		}
	}

	// start millis of a specific month
	public static long getStartOfMonthMillis(int year, int month) {
		LocalDateTime startOfMonth = LocalDateTime.of(year, month, 1, 0, 0, 0, 0);
		return startOfMonth.toInstant(ZoneOffset.UTC).toEpochMilli();
	}

	// end millis of a specific month
	public static long getEndOfMonthMillis(int year, int month) {
		LocalDateTime endOfMonth = LocalDateTime.of(year, month, 1, 23, 59, 59, 999999999)
				.withDayOfMonth(LocalDateTime.of(year, month, 1, 0, 0).toLocalDate().lengthOfMonth());
		return endOfMonth.toInstant(ZoneOffset.UTC).toEpochMilli();
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
			Helper.addCondition(criteria, true, column, operator, value);
		}
		if (criteria.getColumn().size() > 0) {
			criteria.setLogicalOperator("AND");
		}
	}

	public static String getHandler(HttpServletRequest request) {
		String handler = request.getRequestURI();
		handler = handler.substring(handler.lastIndexOf("/") + 1);
		return handler != null ? handler : "";
	}

	public static long convertDateToMillis(String dateString) {
		if (dateString.matches("\\d{13}")) {
			return Long.parseLong(dateString);
		}
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		LocalDate localDate = LocalDate.parse(dateString, formatter);
		return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()).getTime();
	}

	public static Long parseLongOrDefault(String param, Long defaultValue) {
		try {
			return (param != null) ? Long.parseLong(param) : defaultValue;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public static Long parseDateToMillisOrDefault(String param, Long defaultValue) {
		return (param != null) ? Helper.convertDateToMillis(param) : defaultValue;
	}

	public static String generateJwtToken(Map<String, Object> userDetails) {
		Map<String, Object> jwtClaims = new HashMap<>();
		jwtClaims.put("id", userDetails.get("id"));
		jwtClaims.put("role", userDetails.get("role"));
		jwtClaims.put("username", userDetails.get("username"));
		if (userDetails.containsKey("branchId")) {
			jwtClaims.put("branchId", userDetails.get("branchId"));
		}
		return JwtUtil.generateToken(jwtClaims);
	}

	public static Map<String, Object> prepareResponseData(Map<String, Object> userDetails, String jwtToken) {
		Map<String, Object> responseData = new HashMap<>();
		responseData.put("token", jwtToken);
		responseData.putAll(userDetails);
		responseData.put("message", "success");
		return responseData;
	}

	public static <T> T createPojoFromMap(Map<String, Object> map, Class<T> clazz) throws CustomException {
		try {
			T pojo = clazz.getDeclaredConstructor().newInstance();

			for (Map.Entry<String, Object> entry : map.entrySet()) {

				String key = entry.getKey();
				Object value = entry.getValue();
				Field field = null;
				Class<?> currentClass = clazz;
				System.out.println(key + " " + value);
				while (!currentClass.getName().equals("dblayer.model.MarkedClass")) {
					try {
						field = currentClass.getDeclaredField(key);
						break;
					} catch (NoSuchFieldException e) {
						currentClass = currentClass.getSuperclass();
					}
				}
				System.out.println(field.getName() + " " + field.getType());
				field.setAccessible(true);
				if (field.getType() == Long.class || field.getType() == long.class) {
					field.set(pojo, value instanceof Long ? value : Long.parseLong(value.toString()));
				} else if (field.getType() == String.class) {
					field.set(pojo, value.toString());
				} else if (field.getType() == BigDecimal.class) {
					field.set(pojo, new BigDecimal(value.toString()));
				} else if (field.getType() == Boolean.class) {
					field.set(pojo, new Boolean(value.toString()));
				}
			}
			return pojo;
		} catch (Exception e) {
			logger.error("Error while creating POJO of type {} from map. Details: {}", clazz.getSimpleName(),
					e.getMessage(), e);
			throw new CustomException(
					"An unexpected error occurred while processing your request. Please try again later.");

		}
	}

	// Method to capitalize the first letter of a string
	public static String capitalizeFirstLetter(String value) {
		if (value == null || value.isEmpty()) {
			return value;
		}
		return value.substring(0, 1).toUpperCase() + value.substring(1);
	}

	// Method to handle error responses (to streamline error handling in the
	// servlet)
	public static void handleErrorResponse(HttpServletResponse response, int status, String message)
			throws IOException {
		response.setStatus(status);
		response.setContentType("application/json");
		response.getWriter().write("{\"error\": \"" + message + "\"}");
	}

	// Method to parse a JSON body into a JsonObject
	public static JsonObject parseJsonRequest(HttpServletRequest request) throws IOException {
		try (BufferedReader reader = request.getReader()) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		} catch (IOException e) {
			logger.error("Error parsing JSON request", e);
			throw new IOException("Error parsing JSON request", e);
		}
	}

	// cache key generation method for concurrent hashmap.
	public static String generateKey(String entityType, String operation, Object... params) {
		StringJoiner joiner = new StringJoiner(":");
		joiner.add(entityType);
		joiner.add(operation);
		Arrays.stream(params).forEach(param -> joiner.add(String.valueOf(param)));
		return joiner.toString();
	}

	// Method to send JSON responses (commonly used in servlets)
	public static void sendJsonResponse(HttpServletResponse response, int statusCode, String message, Object data)
			throws IOException {
		response.setStatus(statusCode);
		response.setContentType("application/json");
		response.getWriter()
				.write("{\"message\": \"" + message + "\", \"data\": " + (data != null ? data : "{}") + "}");
	}

	public static Map<String, Object> mapJsonObject(JsonObject jsonObject) {
		Map<String, Object> map = new HashMap<>();
		for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
			map.put(entry.getKey(), convertJsonElement(entry.getValue()));
		}
		return map;
	}

	public static Object convertJsonElement(JsonElement element) {
		if (element.isJsonPrimitive()) {
			JsonPrimitive primitive = element.getAsJsonPrimitive();
			if (primitive.isNumber()) {
				Number number = primitive.getAsNumber();
				if (number.doubleValue() == number.longValue()) {
					return number.longValue();
				} else {
					return number.doubleValue();
				}
			} else if (primitive.isBoolean()) {
				return primitive.getAsBoolean();
			} else if (primitive.isString()) {
				return primitive.getAsString();
			}
		}
		return null;
	}

	public static Long parseLong(Object value) {
		if (value instanceof Long) {
			return (Long) value;
		} else if (value instanceof String) {
			try {
				return Long.parseLong((String) value);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Invalid number format: " + value, e);
			}
		} else {
			throw new IllegalArgumentException("Unsupported value type: " + value.getClass().getName());
		}
	}

	public static Map<String, Object> getParametersAsMap(HttpServletRequest request) {
		Map<String, Object> parameterMap = new HashMap<>();

		request.getParameterMap().forEach((key, value) -> {
			if (ValidQueryParams.isValidParam(key)) {
				Object validValue = ValidQueryParams.getValidField(key);
				if (validValue != null) {
					parameterMap.put(key, validValue);
				}
				String paramValue = value != null && value.length > 0 ? value[0] : null;

				if (paramValue != null) {
					Object processedValue = processDynamicKey(key, paramValue);
					parameterMap.put(key, processedValue);
				}
			}
		});

		return parameterMap;
	}

	public static void sendJsonResponse(HttpServletResponse response, HttpStatusCodes statusCode, String message,
			JSONArray jsonArray) throws IOException {
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setStatus(statusCode.getCode());
		JSONObject responseObject = new JSONObject();
		responseObject.put("status", statusCode.getCode());
		responseObject.put("message", message);
		responseObject.put("data", jsonArray != null ? jsonArray : new JSONArray());
		response.getWriter().write(responseObject.toString());
		response.getWriter().flush();
	}

	private static Object processDynamicKey(String key, String paramValue) {
		List<String> Longkeys = new ArrayList<>(Arrays.asList("id", "limit", "accountNumber", "offset", "balance"));
		if (key.toLowerCase().contains("id") || Longkeys.contains(key)) {
			return Helper.parseLongOrDefault(paramValue, 0L);
		}
		if (key.equalsIgnoreCase("from") || key.equalsIgnoreCase("to")) {
			return Helper.parseDateToMillisOrDefault(paramValue, 0L);
		}
		return paramValue;
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
		Criteria criteria = new Criteria();
		criteria.setClazz(clazz);
		if (column != null && operator != null && value != null) {
			criteria.getColumn().add(column);
			criteria.getOperator().add(operator);
			criteria.getValue().add(value);
		}
		return criteria;
	}

	public static ColumnCriteria createColumnCriteria(List<String> fields, List<Object> values) {
		ColumnCriteria columnCriteria = new ColumnCriteria();
		columnCriteria.setFields(fields);
		columnCriteria.setValues(values);
		return columnCriteria;
	}

	public static <T> Criteria initializeCriteria(Class<T> clazz) {
		Criteria criteria = new Criteria();
		criteria.setClazz(clazz);
		criteria.setSelectColumn(new ArrayList<>(Arrays.asList("*")));
		return criteria;
	}

	public static <T> void validateModel(T instance) throws CustomException {
		if (instance == null) {
			throw new CustomException("instance cannot be null.");
		}
		StringBuilder errorMessages = new StringBuilder();
		for (Field field : Transaction.class.getDeclaredFields()) {
			field.setAccessible(true);
			if (field.getName().equals("id")) {
				continue;
			}
			try {
				Object value = field.get(instance);
				if (value == null || (value instanceof String && ((String) value).trim().isEmpty())
						|| (value instanceof BigDecimal && ((BigDecimal) value).compareTo(BigDecimal.ZERO) <= 0)) {
					errorMessages.append("Invalid value for field: ").append(field.getName()).append("\n");
				}
			} catch (IllegalAccessException e) {
				throw new CustomException("Error accessing field: " + field.getName(), e);
			}
		}
		if (errorMessages.length() > 0) {
			throw new CustomException("Validation failed:\n" + errorMessages.toString());
		}
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
			criteria.setJoin(join);
			criteria.setJoinTable(joinTable);
			criteria.setJoinColumn(joinColumn);
			criteria.setJoinOperator(joinOperator);
			criteria.setJoinValue(joinValue);
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
				Helper.addCondition(criteria, true, "RIGHT(account_number, 4)", "EQUAL_TO", accountNumber);
			} else {
				Helper.addConditionIfPresent(criteria, map, "accountNumber", "account_number", "EQUAL_TO", 0L);
			}
		}
	}

}