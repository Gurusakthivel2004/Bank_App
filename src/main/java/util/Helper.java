package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

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
import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import Enum.Constants.HttpStatusCodes;
import Enum.Constants.ValidQueryParams;

import util.ColumnYamlUtil.ClassMapping;

public class Helper {

	private static final Logger logger = LogManager.getLogger(Helper.class);
	private static ThreadLocal<Map<String, Object>> threadLocal = ThreadLocal
			.withInitial(() -> new HashMap<String, Object>());

	public static Map<String, Object> getThreadLocal() {
		return threadLocal.get();
	}

	public static Object getThreadLocalValue(String key) {
		return threadLocal.get().get(key);
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
		return BCrypt.checkpw(password, hashed);
	}

	public static void checkNullValues(Object inputObject) throws CustomException {
		if (inputObject == null) {
			throw new CustomException("Error: Null value provided.", HttpStatusCodes.BAD_REQUEST);
		}
	}

	public static void checkNull(Object obj, String message) throws CustomException {
		if (obj == null) {
			throw new CustomException(message, HttpStatusCodes.BAD_REQUEST);
		}
	}

	public static void checkEmptyString(String str, String message) throws CustomException {
		if (str == null || str.isEmpty()) {
			throw new CustomException(message, HttpStatusCodes.BAD_REQUEST);
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
			throw new CustomException(message, HttpStatusCodes.BAD_REQUEST);
		}
	}

	public static void handleSQLException(SQLException e) throws CustomException {
		logger.error("SQL error: " + e.getMessage(), e);
		throw new CustomException("We encountered an issue while processing your request. Please try again later.",
				HttpStatusCodes.BAD_REQUEST);
	}

	public static void handleGeneralException(Exception e, String message) throws CustomException {
		logger.error(message + ": " + e.getMessage(), e);
		throw new CustomException(message, HttpStatusCodes.INTERNAL_SERVER_ERROR);
	}

	public static void handleGeneralException(Exception e, String logMessage, String message) throws CustomException {
		logger.error("{} {}", logMessage, message, e);
		throw new CustomException(message, e, HttpStatusCodes.INTERNAL_SERVER_ERROR);
	}

	public static void checkPhoneNumber(String number) throws CustomException {
		checkNullValues(number);
		String patternString = "^\\d{10}$";
		if (!Pattern.matches(patternString, number)) {
			throw new CustomException("Error: Mobile number must have 10 digits!", HttpStatusCodes.BAD_REQUEST);
		}
	}

	public static void checkLength(Object[] arr1, Object[] arr2) throws CustomException {
		checkNullValues(arr1);
		checkNullValues(arr2);
		if (arr1.length != arr2.length) {
			throw new CustomException("Both arrays must have the same length.", HttpStatusCodes.BAD_REQUEST);
		}
	}

	public static void checkEmail(String email) throws CustomException {
		checkNullValues(email);
		String patternString = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9-]+\\.[a-zA-Z.]{2,18}$";
		if (!Pattern.matches(patternString, email)) {
			throw new CustomException("Error: Email is not valid", HttpStatusCodes.BAD_REQUEST);
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

	public static void convertMapValuesToLong(Map<String, Object> map) {
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			Object value = entry.getValue();

			if (value instanceof Number) {
				entry.setValue(((Number) value).longValue());
			} else if (value instanceof String) {
				String strValue = (String) value;
				if (isNumeric(strValue)) {
					entry.setValue(Long.parseLong(strValue));
				}
			}
		}
	}

	public static boolean isNumeric(String str) {
		try {
			Long.parseLong(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public static String getHandler(HttpServletRequest request) {
		String handler = request.getRequestURI();
		handler = handler.substring(handler.lastIndexOf("/") + 1);
		return handler != null ? handler : "";
	}

	public static JsonObject parseRequestBody(HttpServletRequest request) throws IOException {
		try (BufferedReader reader = request.getReader()) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		}
	}

	public static void sendSuccessResponse(HttpServletResponse response, String message) throws IOException {
		JsonObject responseJson = new JsonObject();
		responseJson.addProperty("message", message);
		response.setStatus(HttpServletResponse.SC_OK);

		try (PrintWriter out = response.getWriter()) {
			out.print(responseJson.toString());
		}
	}

	public static void sendErrorResponse(HttpServletResponse response, String errorMessage) throws IOException {
		JsonObject responseJson = new JsonObject();
		responseJson.addProperty("message", errorMessage);
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

		try (PrintWriter out = response.getWriter()) {
			out.print(responseJson.toString());
		}
	}

	public static void sendSuccessResponse(HttpServletResponse response, Object responseData) throws IOException {
		response.setContentType("application/json");
		response.setStatus(HttpServletResponse.SC_OK);
		String jsonResponse = new ObjectMapper().writeValueAsString(responseData);
		try (PrintWriter out = response.getWriter()) {
			out.write(jsonResponse);
		}
	}

	public static void sendErrorResponse(HttpServletResponse response, CustomException exception) throws IOException {
		response.setContentType("application/json");
		response.setStatus(exception.getStatusCode());
		JsonObject responseJson = new JsonObject();
		responseJson.addProperty("message", exception.getMessage());
		try (PrintWriter out = response.getWriter()) {
			out.write(responseJson.toString());
		}
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

	// Converts a map to a pojo instance.
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> T createPojoFromMap(Map<String, Object> map, Class<T> clazz) throws CustomException {
		try {
			T pojo = clazz.getDeclaredConstructor().newInstance();

			for (Map.Entry<String, Object> entry : map.entrySet()) {

				String key = entry.getKey();
				Object value = entry.getValue();
				Field field = null;
				Class<?> currentClass = clazz;
				while (!currentClass.getName().equals("dblayer.model.MarkedClass")) {
					try {
						field = currentClass.getDeclaredField(key);
						break;
					} catch (NoSuchFieldException e) {
						currentClass = currentClass.getSuperclass();
					}
				}
				field.setAccessible(true);
				if (field.getType() == Long.class || field.getType() == long.class) {
					if (field.getName().equals("phone") || field.getName().equals("contactNumber")) {
						Helper.checkPhoneNumber((String) value);
					}
					field.set(pojo, value instanceof Long ? value : Long.parseLong(value.toString()));
				} else if (field.getType() == String.class) {
					if (field.getName().equals("email")) {
						Helper.checkEmail((String) value);
					}
					field.set(pojo, value.toString());
				} else if (field.getType() == BigDecimal.class) {
					field.set(pojo, new BigDecimal(value.toString()));
				} else if (field.getType() == Boolean.class) {
					field.set(pojo, Boolean.parseBoolean(value.toString()));
				} else if (field.getType().isEnum()) {
					Class<?> enumClass = field.getType();
					Object enumValue = Enum.valueOf((Class<Enum>) enumClass, value.toString());
					field.set(pojo, enumValue);
				}
			}
			return pojo;
		} catch (CustomException e) {
			throw e;
		} catch (IllegalArgumentException e) {
			logger.error("Invalid argument provided while mapping data to object. Please check the input values.", e);
			throw new CustomException("Invalid data provided. Please check the values and try again.",
					HttpStatusCodes.BAD_REQUEST);
		} catch (IllegalAccessException e) {
			logger.error("Unable to access the field. Ensure the field is accessible and public.", e);
			throw new CustomException(
					"An unexpected error occurred while processing your request. Please contact support.",
					HttpStatusCodes.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			logger.error("Unexpected exception occurred.", e);
			throw new CustomException(
					"An unexpected error occurred while processing your request. Please contact support.",
					HttpStatusCodes.INTERNAL_SERVER_ERROR);
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

	public static String formatFieldName(String fieldName) {
		StringBuilder formattedName = new StringBuilder();
		for (char c : fieldName.toCharArray()) {
			if (Character.isUpperCase(c)) {
				formattedName.append(" ");
			}
			formattedName.append(c);
		}
		return formattedName.toString().trim().substring(0, 1).toUpperCase()
				+ formattedName.toString().trim().substring(1);
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

}