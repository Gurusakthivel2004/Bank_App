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

import Enum.Constants.HttpStatusCodes;
import dao.UserDAO;
import model.ColumnCriteria;
import model.CustomerDetail;
import model.Staff;
import model.User;

import util.CustomException;
import util.Helper;
import util.ValidationUtil;

public class UserService {

	private static final Logger logger = LogManager.getLogger(UserService.class);
	private static UserDAO userDAO = new UserDAO();
	private CacheService cacheService = new CacheService();

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
							{ "email", user.getEmail() }, { "phone", user.getPhone() }, { "role", user.getRole() }, })
					.collect(Collectors.toMap(data -> (String) data[0], data -> data[1]));

			// Add additional staff details for employee role
			if (!"Customer".equals(user.getRole())) {
				logger.info("Fetching additional staff details for employee user.");
				Map<String, Object> userMap = new HashMap<>();
				userMap.put("userId", user.getId());
				userMap.put("role", user.getRole());
				List<Staff> staffDetails = userDAO.getUserDetails(userMap, Staff.class, false);
				if (!staffDetails.isEmpty()) {
					userDetails.put("branchId", staffDetails.get(0).getBranchId());
				} else {
					logger.warn("No staff details found for user with ID: {}", user.getId());
				}
			}
			logger.info("User login successful for username: {}", username);
			return userDetails;
		} catch (CustomException e) {
			logger.error("User login failed for username: {}. Error: {}", username, e.getMessage());
			throw e;
		} catch (Exception e) {
			logger.error("Unexpected error occurred during login.", e);
			throw new CustomException("An unexpected error occurred. Please try again later.",
					HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

	private List<User> checkPassword(String username, String password) throws CustomException {
		logger.info("Validating password for user: {}", username);
		try {
			logger.debug("Fetching user for column: username and value: {}", username);
			List<User> users = null;

			Map<String, Object> userMap = new HashMap<>();
			userMap.put("username", username);
			users = userDAO.getUserDetails(userMap, User.class, false);

			logger.debug("User details cached for username: {}", username);
			logger.debug("Checking password match.");
			User user = users.get(0);

			if (!Helper.checkPassword(password, user.getPassword())) {
				logger.warn("Password mismatch for user: {}", username);
				throw new CustomException("Password does not match", HttpStatusCodes.UNAUTHORIZED);
			}

			logger.info("Password validation successful for user: {}", username);
			return users;
		} catch (CustomException e) {
			logger.error("Password validation failed for user: {}. Error: ", username, e);
			throw e;
		}
	}

	public boolean updatePassword(String currentPassword, String newPassword) throws CustomException {
		logger.info("Attempting to update password.");
		try {
			ColumnCriteria columnCriteria = new ColumnCriteria();
			logger.debug("Hashing new password.");
			columnCriteria.setFields(new ArrayList<>(Arrays.asList("password")));
			columnCriteria.setValues(new ArrayList<>(Arrays.asList(Helper.hashPassword(newPassword))));

			String role = (String) Helper.getThreadLocalValue().get("role");
			logger.debug("Determining update type based on role: {}", role);
			Map<String, Object> userMap = new HashMap<>();
			userMap.put("userId", Helper.getThreadLocalValue().get("id"));
			if ("Customer".equals(role)) {
				logger.debug("Updating password for customer.");
				userDAO.updateUser(columnCriteria, userMap, CustomerDetail.class);
			} else {
				logger.debug("Updating password for staff.");
				userDAO.updateUser(columnCriteria, userMap, Staff.class);
			}
			cacheService.delete("userDetails");
			logger.info("Password updated successfully.");
			return true;
		} catch (CustomException e) {
			logger.error("Password update failed. Error: {}", e.getMessage());
			throw e;
		}
	}

	@SuppressWarnings("unchecked")
	public <T extends User> Map<String, Object> getUserDetails(Map<String, Object> userMap, boolean notExact)
			throws CustomException {
		logger.info("Fetching user details.");
		try {
			Class<T> clazz = (Class<T>) User.class;
			if (userMap.containsKey("userId") && userMap.containsKey("role")) {
				String role = (String) userMap.get("role");
				if (role.equals("Customer")) {
					clazz = (Class<T>) CustomerDetail.class;
				} else {
					clazz = (Class<T>) Staff.class;
				}
			}
			Map<String, Object> usersResult = new HashMap<>();
			Long offset = (Long) userMap.getOrDefault("offset", -1l);
			if (offset == 0) {
				Long count = userDAO.getDataCount(userMap, clazz, notExact);
				usersResult.put("count", count);
			}
			List<T> users = userDAO.getUserDetails(userMap, clazz, notExact);
			usersResult.put("users", users);

			return usersResult;
		} catch (CustomException e) {
			logger.error("Error fetching user details. Error: {}", e);
			throw e;
		} catch (Exception e) {
			logger.error("Unexpected error occurred while fetching user details.", e);
			throw new CustomException("An unexpected error occurred while fetching user details.", e,
					HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

	public void createUser(Map<String, Object> userMap) throws CustomException {
		logger.info("Attempting to create user");
		try {
			String role = (String) userMap.get("role");
			logger.debug("User role determined as: {}", role);
			if ("Customer".equals(role)) {
				logger.debug("Creating customer user.");
				CustomerDetail customerDetail = Helper.createPojoFromMap(userMap, CustomerDetail.class);
				userDAO.createUser(customerDetail, "Customer");
			} else {
				logger.debug("Creating staff user.");
				Staff staff = Helper.createPojoFromMap(userMap, Staff.class);
				userDAO.createUser(staff, "Staff");
			}
			logger.info("User created successfully.");
		} catch (CustomException e) {
			logger.error("Error creating user. User data: {}. Error: {}", userMap, e.getMessage());
			throw e;
		}
	}

	@SuppressWarnings("unchecked")
	public void updateUserDetails(Map<String, Object> userMap) throws CustomException {
		logger.info("Attempting to update user details.");
		try {
			if (!userMap.containsKey("updatedValues")) {
				throw new CustomException("Provide the values to update", HttpStatusCodes.BAD_REQUEST);
			}
			Map<String, Object> updatedValues = (Map<String, Object>) userMap.get("updatedValues");
			Helper.convertMapValuesToLong(updatedValues);
			Class<? extends User> clazz = Staff.class;
			if (userMap.get("role").equals("Customer")) {
				clazz = CustomerDetail.class;
			}
			userMap.remove("role");
			ValidationUtil.validateUpdateFields(updatedValues, clazz);

			List<String> fields = new ArrayList<>();
			List<Object> values = new ArrayList<>();

			String role = (String) userMap.get("role");
			logger.debug("User role determined as: {}", role);

			for (String key : updatedValues.keySet()) {
				fields.add(key);
				values.add(updatedValues.get(key));
			}
			userMap.remove("updatedValues");

			ColumnCriteria columnCriteria = new ColumnCriteria().setFields(fields).setValues(values);
			userDAO.updateUser(columnCriteria, userMap, CustomerDetail.class);
			if ("Customer".equals(role)) {
				logger.debug("Updating details for customer.");
				userDAO.updateUser(columnCriteria, userMap, CustomerDetail.class);
			} else {
				logger.debug("Updating details for staff.");
				userDAO.updateUser(columnCriteria, userMap, Staff.class);
			}
			cacheService.delete("userDetails");
			logger.info("User details updated successfully.");
		} catch (CustomException e) {
			logger.error("Error updating user details. Error: {}", e.getMessage());
			throw e;
		}
	}

}