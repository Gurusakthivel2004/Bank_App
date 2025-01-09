package util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ValidationUtil {

	private static final Logger logger = LogManager.getLogger(ValidationUtil.class);

	private static final List<String> USER_UPDATE_ALLOWED_FIELDS = new ArrayList<>(Arrays.asList("phone", "email",
			"role", "status", "password", "branchId", "fatherName", "motherName", "maritalStatus", "address"));

	private static final List<String> ACCOUNT_UPDATE_ALLOWED_FIELDS = new ArrayList<>(
			Arrays.asList("branchId", "status"));

	private static final List<String> BRANCH_UPDATE_ALLOWED_FIELDS = new ArrayList<>(Arrays.asList("contactNumber"));

	public static void validateUpdateFields(Map<String, Object> inputMap, Class<?> targetClass) {
		logger.info("Starting field validation for class: {}", targetClass.getSimpleName());

		List<String> allowedFields = getAllowedFieldsForClass(targetClass);

		for (Map.Entry<String, Object> entry : inputMap.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			logger.debug("Validating field: {} with value: {}", key, value);

			if (!allowedFields.contains(key)) {
				logger.error("Field '{}' is not allowed for updates.", key);
				throw new IllegalArgumentException("Field '" + key + "' is not allowed for updates.");
			}
			validateFieldType(targetClass, key, value);
		}

		logger.info("Field validation completed successfully for class: {}", targetClass.getSimpleName());
	}

	private static List<String> getAllowedFieldsForClass(Class<?> targetClass) {
		String className = targetClass.getSimpleName().toLowerCase();
		logger.debug("Determining allowed fields for class: {}", className);

		if (className.contains("user")) {
			return USER_UPDATE_ALLOWED_FIELDS;
		} else if (className.contains("account")) {
			return ACCOUNT_UPDATE_ALLOWED_FIELDS;
		} else if (className.contains("branch")) {
			return BRANCH_UPDATE_ALLOWED_FIELDS;
		} else {
			logger.error("No allowed fields found for class: {}", className);
			throw new IllegalArgumentException("No allowed fields found for class: " + className);
		}
	}

	private static void validateFieldType(Class<?> targetClass, String key, Object value) {
		try {
			Field field = getFieldFromClassHierarchy(targetClass, key);

			Class<?> expectedType = field.getType();

			if (!expectedType.isInstance(value)) {
				logger.error("Type mismatch for field '{}': Expected '{}', but received '{}'.", key,
						expectedType.getName(), value.getClass().getName());
				throw new IllegalArgumentException("Type mismatch for field '" + key + "'");
			}
		} catch (NoSuchFieldException e) {
			logger.error("Field '{}' not found in class hierarchy: {}", key, targetClass.getName());
			throw new IllegalArgumentException("Field '" + key + "' not found in class " + targetClass.getName());
		}
	}

	private static Field getFieldFromClassHierarchy(Class<?> clazz, String key) throws NoSuchFieldException {
		while (clazz != null) {
			for (Field field : clazz.getDeclaredFields()) {
				if (field.getName().equals(key)) {
					return field;
				}
			}
			clazz = clazz.getSuperclass();
		}
		logger.error("Field '{}' not found in the class hierarchy.", key);
		throw new NoSuchFieldException("Field '" + key + "' not found in the class hierarchy.");
	}
}
