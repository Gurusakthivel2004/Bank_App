package service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

import Enum.Constants.HttpStatusCodes;
import Enum.Constants.LogType;
import Enum.Constants.Role;
import cache.CacheUtil;
import dao.DAO;
import dao.UserDAO;
import model.ActivityLog;
import model.ColumnCriteria;
import model.CustomerDetail;
import model.Staff;
import model.User;
import util.CustomException;
import util.Helper;
import util.ValidationUtil;

public class UserService {

	private static final Logger logger = LogManager.getLogger(UserService.class);

	private static DAO<User> userDao = new UserDAO<User>();
	private static DAO<CustomerDetail> customerDao = new UserDAO<CustomerDetail>();
	private static DAO<Staff> staffDao = new UserDAO<Staff>();

	private final CacheUtil cacheUtil = new CacheUtil();

	public Map<String, Object> userLogin(String username, String password) throws CustomException {
		logger.info("Attempting login for username: {}", username);
		try {
			List<User> users = checkPassword(username, password);

			if (users == null || users.isEmpty() || users.size() > 1) {
				logger.warn("User not found or invalid credentials for username: {}", username);
				throw new CustomException("Invalid username or password.", HttpStatusCodes.UNAUTHORIZED);
			}

			User user = users.get(0);
			Map<String, Object> userDetails = Stream
					.of(new Object[][] { { "id", user.getId() }, { "fullname", user.getFullname() },
							{ "username", user.getUsername() }, { "status", user.getStatus() },
							{ "email", user.getEmail() }, { "phone", user.getPhone() }, { "role", user.getRole() } })
					.collect(Collectors.toMap(data -> (String) data[0], data -> data[1]));

			if (!(Role.Customer == user.getRoleEnum())) {
				addStaffDetails(userDetails, user);
			}

			ActivityLog activityLog = new ActivityLog().setLogMessage("Login").setLogType(LogType.Login)
					.setUserAccountNumber(null).setRowId(user.getId()).setTableName("User").setUserId(user.getId())
					.setPerformedBy(user.getId());

			TaskExecutorService.getInstance().submit(activityLog);

			logger.info("User login successful for username: {}", username);
			return userDetails;

		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			logger.error("Unexpected error occurred during login for username: {}", username, e);
			throw new CustomException("An unexpected error occurred. Please try again later.",
					HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

	private void addStaffDetails(Map<String, Object> userDetails, User user) throws CustomException {
		logger.info("Fetching additional staff details for employee user.");
		try {
			Map<String, Object> userMap = new HashMap<>();
			userMap.put("userId", user.getId());
			userMap.put("role", user.getRoleEnum());
			userMap.put("userClass", Staff.class);

			List<Staff> staffDetails = staffDao.get(userMap);

			if (!staffDetails.isEmpty()) {
				userDetails.put("branchId", staffDetails.get(0).getBranchId());
			} else {
				logger.warn("No staff details found for user with ID: {}", user.getId());
			}
		} catch (Exception e) {
			logger.error("Error fetching staff details for user with ID: {}", user.getId(), e);
			throw new CustomException("Unable to fetch staff details. Please try again later.",
					HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

	private List<User> checkPassword(String username, String password) throws CustomException {
		logger.info("Validating password for user: {}", username);
		try {
			List<User> users = null;

			Map<String, Object> userMap = new HashMap<>();
			userMap.put("username", username);
			userMap.put("userClass", User.class);
			users = userDao.get(userMap);

			if (users == null || users.size() == 0) {
				throw new CustomException("User not found", HttpStatusCodes.BAD_REQUEST);
			}

			User user = users.get(0);
			if (user.getStatus() != "Active") {
				logger.error("User suspended: {}", username);
				throw new CustomException("User suspended", HttpStatusCodes.UNAUTHORIZED);
			}

			if (!Helper.checkPassword(password, user.getPassword())) {
				logger.error("Password mismatch for user: {}", username);
				throw new CustomException("Password does not match", HttpStatusCodes.UNAUTHORIZED);
			}

			logger.info("Password validation successful for user: {}", username);
			return users;
		} catch (CustomException e) {
			logger.error("Password validation failed for user: {}. Error: ", username, e);
			throw e;
		}
	}

	public void updatePassword(Map<String, Object> passwordMap) throws CustomException {
		logger.info("Attempting to update password.");
		try {
			String currentPassword = (String) passwordMap.get("currentPassword"),
					newPassword = (String) passwordMap.get("newPassword");

			ValidationUtil.validatePassword(newPassword);
			Map<String, Object> userMap = new HashMap<>();
			Long userId = (Long) Helper.getThreadLocalValue("id");
			userMap.put("userId", userId);
			userMap.put("userClass", User.class);
			List<User> users = userDao.get(userMap);

			User user = users.get(0);
			if (!Helper.checkPassword(currentPassword, user.getPassword())) {
				logger.error("Password mismatch for user");
				throw new CustomException("Please enter correct password", HttpStatusCodes.UNAUTHORIZED);
			}

			ColumnCriteria columnCriteria = new ColumnCriteria().setFields(new ArrayList<>(Arrays.asList("password")))
					.setValues(new ArrayList<>(Arrays.asList(Helper.hashPassword(newPassword))));

			Role role = Role.fromString((String) Helper.getThreadLocalValue("role"));

			if (role == Role.Customer) {
				userMap.put("userClass", CustomerDetail.class);
				userDao.update(columnCriteria, userMap);
			} else {
				userMap.put("userClass", Staff.class);
				userDao.update(columnCriteria, userMap);
			}

			cacheUtil.delete("userDetails");
			logger.info("Password updated successfully.");

			ActivityLog activityLog = new ActivityLog().setLogMessage("password updated").setLogType(LogType.Update)
					.setUserAccountNumber(null).setRowId(userId).setTableName("User").setUserId(userId);

			TaskExecutorService.getInstance().submit(activityLog);
		} catch (CustomException e) {
			logger.error("Password update failed. Error: {}", e.getMessage());
			throw e;
		}
	}

	@SuppressWarnings("unchecked")
	public <T extends User> Map<String, Object> getUserDetails(Map<String, Object> userMap) throws CustomException {
		logger.info("Fetching user details.");
		try {
			String key = "userDetails";
			List<Object> cachedUsers = cacheUtil.getCachedList(key, new TypeReference<List<Object>>() {
			}, userMap, "userId");

			if (cachedUsers != null && userMap.size() == 1) {
				return createResultMap(cachedUsers, null);
			}

			DAO<?> dao = userDao;
			Class<T> clazz = (Class<T>) User.class;

			if (userMap.containsKey("userId") && userMap.containsKey("role")) {
				Role role = Role.fromString((String) userMap.get("role"));
				if (role == Role.Customer) {
					dao = customerDao;
					clazz = (Class<T>) CustomerDetail.class;
				} else {
					dao = staffDao;
					clazz = (Class<T>) Staff.class;
				}
				userMap.put("role", role);
			}

			userMap.put("userClass", clazz);

			Long offset = (Long) userMap.getOrDefault("offset", -1L);
			Long count = (offset == 0) ? dao.getDataCount(userMap) : null;

			List<?> users = dao.get(userMap);

			if (!userMap.containsKey("notExact") && userMap.containsKey("userId")) {
				cacheUtil.save(key + userMap.get("userId"), users);
			}

			return createResultMap(users, count);

		} catch (Exception e) {
			logger.error("Error fetching user details.", e);
			throw new CustomException("Unexpected error occurred.", e, HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

	private Map<String, Object> createResultMap(List<?> users, Long count) {
		Map<String, Object> resultMap = new HashMap<>();
		resultMap.put("users", users);
		if (count != null) {
			resultMap.put("count", count);
		}
		return resultMap;
	}

	public void createUser(Map<String, Object> userMap) throws CustomException {
		logger.info("Attempting to create user");
		try {
			Role role = Role.fromString((String) userMap.get("role"));
			Long userId;
			if (role == Role.Customer) {
				CustomerDetail customerDetail = Helper.createPojoFromMap(userMap, CustomerDetail.class);
				ValidationUtil.validateCustomerModel(customerDetail);
				userId = userDao.create(customerDetail);
			} else {
				Staff staff = Helper.createPojoFromMap(userMap, Staff.class);
				ValidationUtil.validateStaffModel(staff);
				userId = userDao.create(staff);
			}
			logger.info("User created successfully.");

			ActivityLog activityLog = new ActivityLog().setLogMessage("User created").setLogType(LogType.Insert)
					.setUserAccountNumber(null).setRowId(userId).setTableName("User").setUserId(userId);

			TaskExecutorService.getInstance().submit(activityLog);
		} catch (CustomException e) {
			logger.error("Error creating user. User data: {}. Error: {}", userMap, e);
			throw e;
		}
	}

	@SuppressWarnings("unchecked")
	public void updateUserDetails(Map<String, Object> userMap) throws CustomException {
		logger.info("Attempting to update user details.");
		try {
			if (userMap.containsKey("currentPassword")) {
				updatePassword(userMap);
				return;
			}
			if (!userMap.containsKey("updatedValues") || !userMap.containsKey("userId")) {
				throw new CustomException("Provide the values to update", HttpStatusCodes.BAD_REQUEST);
			}

			Long userId = Long.parseLong((String) userMap.get("userId"));
			Map<String, Object> updatedValues = (Map<String, Object>) userMap.get("updatedValues");
			Helper.convertMapValuesToLong(updatedValues);

			Class<? extends User> clazz = Staff.class;
			if (Role.fromString((String) userMap.get("role")) == Role.Customer) {
				clazz = CustomerDetail.class;
			}

			ValidationUtil.validateUpdateFields(updatedValues, clazz);

			List<String> fields = new ArrayList<>();
			List<Object> values = new ArrayList<>();
			StringBuilder logMessage = new StringBuilder("updated fields: ");
			StringBuilder logValues = new StringBuilder("with values: ");

			Role role = Role.fromString((String) userMap.get("role"));
			userMap.remove("role");

			for (String key : updatedValues.keySet()) {

				if ("modifiedAt".equals(key) || "performedBy".equals(key)) {
					continue;
				}

				fields.add(key);
				values.add(updatedValues.get(key));

				logMessage.append(key).append(", ");
				logValues.append(updatedValues.get(key)).append(", ");

			}

			if (logMessage.length() > 0) {
				logMessage.setLength(logMessage.length() - 2);
				logValues.setLength(logValues.length() - 2);
			}
			userMap.remove("updatedValues");

			// Use validation check.
			List<User> users = (List<User>) getUserDetails(userMap).get("users");
			if (users == null || users.isEmpty()) {
				throw new CustomException("User not found.", HttpStatusCodes.BAD_REQUEST);
			}

			ColumnCriteria columnCriteria = new ColumnCriteria().setFields(fields).setValues(values);
			if (role == Role.Customer) {
				userMap.put("userClass", CustomerDetail.class);
				customerDao.update(columnCriteria, userMap);
			} else {
				userMap.put("userClass", Staff.class);
				staffDao.update(columnCriteria, userMap);
			}
			cacheUtil.delete("userDetails");
			logger.info("User details updated successfully.");

			ActivityLog activityLog = new ActivityLog()
					.setLogMessage(logMessage.toString() + " " + logValues.toString()).setLogType(LogType.Update)
					.setUserAccountNumber(null).setRowId(userId).setTableName("User").setUserId(userId);

			TaskExecutorService.getInstance().submit(activityLog);

		} catch (CustomException e) {
			logger.error("Error updating user details. Error: {}", e.getMessage());
			throw e;
		}
	}

}