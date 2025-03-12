package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.password4j.Password;

import enums.Constants.HttpStatusCodes;
import enums.Constants.Role;
import enums.Constants.ValidQueryParams;
import io.github.cdimascio.dotenv.Dotenv;
import model.User;
import service.UserService;
import util.ColumnYamlUtil.ClassMapping;

public class Helper {

	private static Logger LOGGER = LogManager.getLogger(Helper.class);

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

	public static String hashPassword(String password, int passwordVersion) {
		switch (passwordVersion) {
		case 0:
			return BCrypt.hashpw(password, BCrypt.gensalt());
		case 1:
			return Password.hash(password).addRandomSalt().withArgon2().getResult();
		default:
			throw new IllegalArgumentException("Invalid password version");
		}
	}

	public static boolean checkPassword(User user, String password) throws Exception {
		int version = user.getPasswordVersion();

		if (version == 0) {
			return migratePasswordIfValid(user, password);
		} else if (version == 1) {
			return Password.check(password, user.getPassword()).withArgon2();
		} else {
			throw new IllegalArgumentException("Invalid password version: " + version);
		}
	}

	private static boolean migratePasswordIfValid(User user, String password) throws Exception {
		if (BCrypt.checkpw(password, user.getPassword())) {
			UserService.getInstance().updateUserPassword(user.getId(), password, user.getRole());
			return true;
		}
		return false;
	}

	public static void checkNullValues(Object inputObject) throws CustomException {
		if (inputObject == null) {
			throw new CustomException("Error: Null value provided.", HttpStatusCodes.BAD_REQUEST);
		}
	}

	public static void checkNullValues(Object inputObject, String key) throws CustomException {
		if (inputObject == null) {
			throw new CustomException(key, HttpStatusCodes.BAD_REQUEST);
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
		LOGGER.error("SQL error: " + e.getMessage(), e);
		throw new CustomException("We encountered an issue while processing your request. Please try again later.",
				HttpStatusCodes.INTERNAL_SERVER_ERROR);
	}

	public static void handleGeneralException(Exception e, String message) throws CustomException {
		LOGGER.error(message + ": " + e.getMessage(), e);
		throw new CustomException(message, HttpStatusCodes.INTERNAL_SERVER_ERROR);
	}

	public static void handleGeneralException(Exception e, String logMessage, String message) throws CustomException {
		LOGGER.error("{} {}", logMessage, message, e);
		throw new CustomException(message, e, HttpStatusCodes.INTERNAL_SERVER_ERROR);
	}

	public static void checkPhoneNumber(String number) throws CustomException {
		checkNullValues(number, "Please enter phone number.");
		number = number.trim();

		String patternString = "^[6-9]\\d{9}$";
		if (!Pattern.matches(patternString, number)) {
			throw new CustomException("Error: Mobile number must have 10 digits!", HttpStatusCodes.BAD_REQUEST);
		}
	}

	public static void checkUsername(String username) throws CustomException {
		checkNullValues(username, "Please enter username.");

		String patternString = "^(?=.*[a-zA-Z])[a-zA-Z0-9_]{5,}$";

		if (!Pattern.matches(patternString, username)) {
			throw new CustomException(
					"Error: Username must be at least 5 characters long, contain at least one alphabet, and can only include letters, numbers, and underscores!",
					HttpStatusCodes.BAD_REQUEST);
		}
	}

	public static void checkPanNumber(String panNumber) throws CustomException {
		checkNullValues(panNumber, "Please enter pan number.");
		String patternString = "^[a-zA-Z0-9]{10,}$";
		if (!Pattern.matches(patternString, panNumber)) {
			throw new CustomException(
					"Error: pan number must be at least 10 characters long and can only contain numbers.",
					HttpStatusCodes.BAD_REQUEST);
		}
	}

	public static void checkStartingBalance(long amount, long minBalance) throws CustomException {
		checkNullValues(amount, "Please enter amount.");
		if (amount < minBalance) {
			throw new CustomException("Error: Must be greater than minimum balance.", HttpStatusCodes.BAD_REQUEST);
		}
	}

	public static void checkAddress(String address) throws CustomException {
		checkNullValues(address, "Please enter address.");
		address = address.trim();
		String patternString = "^[a-zA-Z0-9.,\\-/()\\s]+$";
		LOGGER.debug(address);
		LOGGER.debug(Pattern.matches(patternString, address));

		if (!Pattern.matches(patternString, address) || address.length() < 10 || address.length() > 100) {
			throw new CustomException("Error: Enter a valid address.", HttpStatusCodes.BAD_REQUEST);
		}

		if (!address.contains(" ")) {
			throw new CustomException("Error: Address must contain at least one space!", HttpStatusCodes.BAD_REQUEST);
		}
	}

	public static void checkName(String fullname) throws CustomException {
		checkNullValues(fullname, "Please enter full name.");

		String patternString = "^[A-Za-z]+(?: [A-Za-z]+)*$";

		if (fullname.length() < 2 || fullname.length() > 20) {
			throw new CustomException("Error: Full name must be between 2 and 20 characters long.",
					HttpStatusCodes.BAD_REQUEST);
		}

		if (!Pattern.matches(patternString, fullname)) {
			throw new CustomException(
					"Error: Name must only contain alphabets and single spaces between words. No special characters are allowed.",
					HttpStatusCodes.BAD_REQUEST);
		}
	}

	public static void checkAadharNumber(String aadharNumber) throws CustomException {
		checkNullValues(aadharNumber, "Please enter aadhar number.");
		String patternString = "^[0-9]{10,}$";
		if (!Pattern.matches(patternString, aadharNumber)) {
			throw new CustomException(
					"Error: aadhar number must be at least 10 characters long and can only contain letters and numbers.",
					HttpStatusCodes.BAD_REQUEST);
		}
	}

	public static void checkDOB(String dob) throws CustomException {
		Helper.checkNullValues(dob, "Please enter dob.");
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		LocalDate birthDate;

		try {
			birthDate = LocalDate.parse(dob, formatter);
		} catch (DateTimeParseException e) {
			throw new CustomException("Error: Date of birth must be in dd/MM/yyyy format!",
					HttpStatusCodes.BAD_REQUEST);
		}

		LocalDate today = LocalDate.now();

		if (birthDate.isAfter(today)) {
			throw new CustomException("Error: Date of birth cannot be in the future!", HttpStatusCodes.BAD_REQUEST);
		}

		int age = Period.between(birthDate, today).getYears();
		if (age < 18) {
			throw new CustomException("Error: Age must be at least 18 years old!", HttpStatusCodes.BAD_REQUEST);
		}
		if (age > 150) {
			throw new CustomException("Error: Enter valid age!", HttpStatusCodes.BAD_REQUEST);
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
		checkNullValues(email, "Please enter email.");

		String patternString = "^(?=.{1,254}$)[a-zA-Z0-9]+(?:[._%+-][a-zA-Z0-9]+)*@([a-zA-Z0-9-]+)\\.([a-zA-Z]{2,18})$";

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

	public static String decryptAES(String encryptedText, String ivBase64) throws Exception {
		Dotenv dotenv = loadDotEnv();
		String SECRET_KEY = dotenv.get("AES_SECRET_KEY");

		byte[] keyBytes = Arrays.copyOf(SECRET_KEY.getBytes("UTF-8"), 32);
		SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");

		byte[] ivBytes = Base64.getDecoder().decode(ivBase64);
		IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);

		byte[] encryptedBytes = Base64.getDecoder().decode(encryptedText);

		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

		byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
		return new String(decryptedBytes, "UTF-8");
	}

	public static boolean isJwtToken(String token) {
		return token.split("\\.").length == 3;
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

	public static String sendPostRequest(String url, String params) throws IOException {
		URL obj = new URL(url);
		HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

		OutputStream os = conn.getOutputStream();
		os.write(params.getBytes());
		os.flush();
		os.close();

		return readResponse(conn);
	}

	public static String sendGetRequest(String url) throws IOException {
		URL obj = new URL(url);
		HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
		conn.setRequestMethod("GET");
		return readResponse(conn);
	}

	public static String readResponse(HttpURLConnection conn) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String inputLine;
		StringBuilder response = new StringBuilder();
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		return response.toString();
	}

	public static void sendSuccessResponse(HttpServletResponse response, Object responseData) throws IOException {
		response.setContentType("application/json");
		response.setStatus(HttpServletResponse.SC_OK);
		String jsonResponse = new ObjectMapper().writeValueAsString(responseData);
		try (PrintWriter out = response.getWriter()) {
			out.write(jsonResponse);
		}
		response.flushBuffer();
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

	public static String getFromCookies(HttpServletRequest request, String key) {
		if (request.getCookies() != null) {
			for (Cookie cookie : request.getCookies()) {
				if (key.equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}
		return null;
	}

	public static Map<String, Object> getClaimsFromId(Long userId) throws Exception {
		if (userId == null) {
			throw new IllegalArgumentException("User ID cannot be null or empty");
		}

		User user = UserService.getInstance().getUserById(userId);
		Role role = user.getRoleEnum();

		Map<String, Object> claimsMap = new HashMap<>();
		claimsMap.put("id", userId);
		claimsMap.put("role", user.getRole());
		claimsMap.put("username", user.getUsername());

		if (role != Role.Customer) {
			UserService.getInstance().addStaffDetails(claimsMap, user);
		}
		return claimsMap;
	}

	public static Long parseDateToMillisOrDefault(String param, Long defaultValue) {
		return (param != null) ? Helper.convertDateToMillis(param) : defaultValue;
	}

	public static Dotenv loadDotEnv() {
		return Dotenv.configure().directory("/home/guru-pt7672/git/BankApplication/Bank_Application").load();
	}

	public static void setCookie(HttpServletResponse response, String name, Object value, int maxAge,
			boolean httpOnly) {
		try {
			String encodedValue = URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8.toString());
			Cookie cookie = new Cookie(name, encodedValue);
			cookie.setMaxAge(maxAge);
			cookie.setHttpOnly(httpOnly);
			cookie.setPath("/Bank_Application");
			response.addCookie(cookie);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void updateCookie(HttpServletResponse response, String accessToken) {
		Cookie oldCookie = new Cookie("token", "");
		oldCookie.setMaxAge(0);
		oldCookie.setPath("/Bank_Application");
		response.addCookie(oldCookie);

		Cookie newCookie = new Cookie("token", accessToken);
		newCookie.setMaxAge(60 * 60 * 24);
		newCookie.setPath("/");
		response.addCookie(newCookie);
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
			LOGGER.error("Invalid argument provided while mapping data to object. Please check the input values.", e);
			throw new CustomException("Invalid data provided. Please check the values and try again.",
					HttpStatusCodes.BAD_REQUEST);
		} catch (IllegalAccessException e) {
			LOGGER.error("Unable to access the field. Ensure the field is accessible and public.", e);
			throw new CustomException(
					"An unexpected error occurred while processing your request. Please contact support.",
					HttpStatusCodes.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			LOGGER.error("Unexpected exception occurred.", e);
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
			LOGGER.error("Error parsing JSON request", e);
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
			JsonArray jsonArray) throws IOException {
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setStatus(statusCode.getCode());
		JsonObject responseObject = new JsonObject();
		responseObject.addProperty("status", statusCode.getCode());
		responseObject.addProperty("message", message);
		responseObject.addProperty("data", jsonArray != null ? jsonArray.toString() : new JsonArray().toString());
		response.getWriter().write(responseObject.toString());
		response.getWriter().flush();
	}

	private static Object processDynamicKey(String key, String paramValue) {
		List<String> Longkeys = new ArrayList<>(Arrays.asList("id", "limit", "accountNumber", "offset", "balance"));
		if (key.toLowerCase().contains("id") || Longkeys.contains(key)) {
			System.out.println(key + " " + paramValue);
			return Helper.parseLong(paramValue);
		}
		if (key.equalsIgnoreCase("from") || key.equalsIgnoreCase("to")) {
			return Helper.parseDateToMillisOrDefault(paramValue, 0L);
		}
		return paramValue;
	}

}