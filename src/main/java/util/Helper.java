package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dblayer.model.Criteria;
import service.BranchService;

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
		return BCrypt.checkpw(password, hashed);
	}

	public static void checkNullValues(Object inputObject) throws CustomException {
		if (inputObject == null) {
			throw new CustomException("Error: Null value provided.");
		}
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
	}

	public static String getHandler(HttpServletRequest request) {
		String handler = request.getRequestURI();
		handler = handler.substring(handler.lastIndexOf("/") + 1); // Extract the part after last '/'
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
				if (primitive.getAsNumber() instanceof Long) {
					return primitive.getAsLong();
				} else {
					return primitive.getAsDouble();
				}
			} else if (primitive.isBoolean()) {
				return primitive.getAsBoolean();
			} else if (primitive.isString()) {
				return primitive.getAsString();
			}
		}
		return null;
	}

}