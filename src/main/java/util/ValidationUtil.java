package util;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator.OfDouble;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import Enum.Constants.HttpStatusCodes;
import Enum.Constants.Role;
import Enum.Constants.Status;
import dao.BranchDAO;
import dao.DAO;
import dao.UserDAO;
import model.Account;
import model.Branch;
import model.CustomerDetail;
import model.MarkedClass;
import model.Staff;
import model.Transaction;
import model.User;
import service.BranchService;

public class ValidationUtil {

	private static final Logger logger = LogManager.getLogger(ValidationUtil.class);

	private static final List<String> USER_UPDATE_ALLOWED_FIELDS = Arrays.asList("phone", "email", "role", "status",
			"password", "branchId", "fatherName", "motherName", "maritalStatus", "address");

	private static final List<String> ACCOUNT_UPDATE_ALLOWED_FIELDS = Arrays.asList("branchId", "status");

	private static final List<String> BRANCH_UPDATE_ALLOWED_FIELDS = Arrays.asList("contactNumber");

	public static void validateUpdateFields(Map<String, Object> inputMap, Class<?> targetClass) throws CustomException {
		logger.info("Starting field validation for class: {}", targetClass.getSimpleName());

		List<String> allowedFields = getAllowedFieldsForClass(targetClass);

		for (Map.Entry<String, Object> entry : inputMap.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			logger.debug("Validating field: {} with value: {}", key, value);

			if (!allowedFields.contains(key)) {
				logger.error("Field '{}' is not allowed for updates.", key);
				throw new CustomException("Field '" + key + "' is not allowed for updates.",
						HttpStatusCodes.BAD_REQUEST);
			}
			try {
				validateFieldType(targetClass, key, value);
			} catch (Exception e) {
				e.printStackTrace();
				throw new CustomException("Please check your inputs and try again.", HttpStatusCodes.BAD_REQUEST);
			}
		}

		logger.info("Field validation completed successfully for class: {}", targetClass.getSimpleName());
	}

	private static List<String> getAllowedFieldsForClass(Class<?> targetClass) {
		String className = targetClass.getSimpleName();
		logger.debug("Determining allowed fields for class: {}", className);
		if (className.contains("User") || className.contains("CustomerDetail") || className.contains("Staff")) {
			return USER_UPDATE_ALLOWED_FIELDS;
		} else if (className.contains("Account")) {
			return ACCOUNT_UPDATE_ALLOWED_FIELDS;
		} else if (className.contains("Branch")) {
			return BRANCH_UPDATE_ALLOWED_FIELDS;
		} else {
			logger.error("No allowed fields found for class: {}", className);
			throw new IllegalArgumentException("No allowed fields found for class: " + className);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void validateFieldType(Class<?> targetClass, String key, Object value) throws CustomException {
		try {
			Field field = getFieldFromClassHierarchy(targetClass, key);
			Class<?> expectedType = field.getType();
			validateKey(key, value);
			// Check if the expected type is an enum and the value is a String
			if (expectedType.isEnum() && value instanceof String) {
				try {
					Object enumValue = Enum.valueOf((Class<Enum>) expectedType, (String) value);
					if (!expectedType.isInstance(enumValue)) {
						logger.error("Type mismatch for field '{}': Expected '{}', but received '{}'.", key,
								expectedType.getName(), value.getClass().getName());
						throw new CustomException("Type mismatch for field '" + key + "'", HttpStatusCodes.BAD_REQUEST);
					}
				} catch (IllegalArgumentException e) {
					logger.error("Invalid value for field '{}': Expected one of '{}', but received '{}'.", key,
							Arrays.toString(expectedType.getEnumConstants()), value);
					throw new CustomException("Invalid enum value for field '" + key + "'",
							HttpStatusCodes.BAD_REQUEST);
				}
			} else if (!expectedType.isInstance(value)) {
				logger.error("Type mismatch for field '{}': Expected '{}', but received '{}'.", key,
						expectedType.getName(), value.getClass().getName());
				throw new CustomException("Type mismatch for field '" + key + "'", HttpStatusCodes.BAD_REQUEST);
			}
		} catch (NoSuchFieldException e) {
			logger.error("Field '{}' not found in class hierarchy: {}", key, targetClass.getName());
			throw new CustomException("Field '" + key + "' not found in class " + targetClass.getName(),
					HttpStatusCodes.BAD_REQUEST);
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

	public static <T> void validateModel(T instance, Class<? extends MarkedClass> clazz) throws CustomException {
		if (instance == null) {
			throw new CustomException("Instance cannot be null.", HttpStatusCodes.BAD_REQUEST);
		}

		StringBuilder errorMessages = new StringBuilder();
		for (Field field : clazz.getDeclaredFields()) {
			field.setAccessible(true);
			if (field.getName().equals("id")) {
				continue;
			}
			try {
				Object value = field.get(instance);
				if (value == null || (value instanceof String && ((String) value).trim().isEmpty())
						|| (value instanceof BigDecimal && ((BigDecimal) value).compareTo(BigDecimal.ZERO) < 0)) {

					String formattedFieldName = Helper.formatFieldName(field.getName());
					errorMessages.append("Invalid value for field: ").append(formattedFieldName).append("\n");
				}
			} catch (IllegalAccessException e) {
				throw new CustomException("Error accessing field: " + field.getName(), e,
						HttpStatusCodes.INTERNAL_SERVER_ERROR);
			}
		}

		if (errorMessages.length() > 0) {
			throw new CustomException("Validation failed:\n" + errorMessages.toString(), HttpStatusCodes.BAD_REQUEST);
		}
	}

	public static boolean getAssignedBranches(List<Account> accounts, Long branchId) {
		if (accounts == null || accounts.isEmpty()) {
			return true;
		}
		return accounts.stream().anyMatch(account -> account.getBranchId().equals(branchId));
	}

	public static boolean getAssignedTransactions(List<Transaction> transactions, Long branchId)
			throws CustomException {
		if (transactions == null || transactions.isEmpty()) {
			return true;
		}
		BranchService branchService = new BranchService();
		Map<String, Object> branchMap = new HashMap<>();
		branchMap.put("branchId", branchId);
		Branch branchDetails = branchService.getBranchDetails(branchMap).get(0);
		return transactions.stream().allMatch(transaction -> transaction.getIfsc().equals(branchDetails.getIfscCode()));
	}

	public static void validateCustomerModel(CustomerDetail user) throws CustomException {
		Helper.checkEmail(user.getEmail());
		Helper.checkPhoneNumber(user.getPhone().toString());
		Helper.checkUsername(user.getUsername());
		Helper.checkAadharNumber(user.getAadharNumber().toString());
		Helper.checkPanNumber(user.getPanNumber());
		Helper.checkFullName(user.getFullname());
		Helper.checkDOB(user.getDob());
		Helper.checkAddress(user.getAddress());
	}

	public static void validateStaffModel(Staff user) throws CustomException {
		Helper.checkEmail(user.getEmail());
		Helper.checkPhoneNumber(user.getPhone().toString());
		Helper.checkUsername(user.getUsername());
	}

	public static void validateBranchModel(Branch branch) throws CustomException {
		Helper.checkPhoneNumber(branch.getContactNumber().toString());
		Helper.checkNullValues(branch.getName(), "Please enter branch name.");
		Helper.checkNullValues(branch.getAddress(), "Please enter branch address.");
	}

	public static void userExists(Long userId) throws CustomException {
		Map<String, Object> userMap = new HashMap<>();
		userMap.put("userId", userId);
		userMap.put("userClass", User.class);
		DAO<User> userDAO = new UserDAO<User>();
		List<User> users = userDAO.get(userMap);
		if (users == null || users.size() == 0) {
			throw new CustomException("No User found with the user Id", HttpStatusCodes.BAD_REQUEST);
		}
	}

	public static void branchExists(Long branchId) throws CustomException {
		Map<String, Object> branchMap = new HashMap<>();
		branchMap.put("id", branchId);
		DAO<Branch> branchDAO = new BranchDAO();
		List<Branch> branches = branchDAO.get(branchMap);
		if (branches == null || branches.size() == 0) {
			throw new CustomException("No Branch found with the branch Id", HttpStatusCodes.BAD_REQUEST);
		}
	}

	public static void validateCreateAccount(Map<String, Object> accountMap) throws CustomException {
		if (!accountMap.containsKey("userId")) {
			throw new CustomException("User Id not found!. Please enter user id", HttpStatusCodes.BAD_REQUEST);
		} else if (!accountMap.containsKey("balance")) {
			throw new CustomException("Please enter balance", HttpStatusCodes.BAD_REQUEST);
		} else if (!accountMap.containsKey("accountType")) {
			throw new CustomException("Account type not found!. Please enter account type",
					HttpStatusCodes.BAD_REQUEST);
		}
		ValidationUtil.userExists(Long.parseLong((String) accountMap.get("userId")));
	}

	public static void validatePassword(String password) throws CustomException {
		Helper.checkNullValues(password, "Please enter password.");
		String patternString = "^[0-9a-zA-Z_]{5,}$";
		if (!Pattern.matches(patternString, password)) {
			throw new CustomException("Password must atleast 5 characters containing alphabets, numbers and underscre.",
					HttpStatusCodes.BAD_REQUEST);
		}
	}

	public static void validateKey(String key, Object value) throws CustomException {
		switch (key) {
		case "email":
			Helper.checkEmail(value.toString());
			break;
		case "phone":
		case "contactNumber":
			Helper.checkPhoneNumber(key);
			break;
		case "branchId":
			branchExists((Long) value);
		case "fullname":
		case "motherName":
		case "fatherName":
			Helper.checkFullName(value.toString());
		case "address":
			Helper.checkAddress(value.toString());
		default:
			break;
		}
	}

}