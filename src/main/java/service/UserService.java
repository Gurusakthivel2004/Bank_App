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
import dblayer.dao.UserDAO;
import dblayer.model.ColumnCriteria;
import dblayer.model.Staff;
import dblayer.model.User;
import util.CustomException;
import util.Helper;

public class UserService {

	private static final Logger logger = LogManager.getLogger(UserService.class);
	private static UserDAO userDAO = new UserDAO();
	private CacheService cacheService = new CacheService();

	@SuppressWarnings("unchecked")
	public Map<String, Object> userLogin(String username, String password) throws CustomException {
		logger.info("Attempting login for username: {}", username);

		try {
			Map<String, Object> users = checkPassword(username, password);
			if (users == null || users.isEmpty() || users.size() > 1) {
				logger.warn("User not found or invalid credentials for username: {}", username);
				throw new CustomException("Invalid username or password.");
			}
			User user = (User) ((List<Object>) users.get("users")).get(0);
			Map<String, Object> userDetails = Stream
					.of(new Object[][] { { "id", user.getId() }, { "fullname", user.getFullname() },
							{ "username", user.getUsername() }, { "status", user.getStatus() },
							{ "email", user.getEmail() }, { "phone", user.getPhone() }, { "role", user.getRole() }, })
					.collect(Collectors.toMap(data -> (String) data[0], data -> data[1]));

			// Add additional staff details for employee role
			if (!"Customer".equals(user.getRole())) {
				logger.debug("Fetching additional staff details for employee user.");
				Map<String, Object> staffDetails = userDAO.getStaff(user.getId());
				if (!staffDetails.isEmpty()) {
					userDetails.put("branchId",
							((Staff) ((List<Object>) staffDetails.get("staff")).get(0)).getBranchId());
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
			logger.error("Unexpected error occurred during login.}", e);
			throw new CustomException("An unexpected error occurred. Please try again later.", e);
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> checkPassword(String username, String password) throws CustomException {
		logger.info("Validating password for user: {}", username);
		try {
			logger.debug("Fetching user for column: username and value: {}", username);
			String key = "userDetails";
			Map<String, Object> users = null;
			Map<String, Map<String, Object>> cachedUserDetails = cacheService.get(key,
					new TypeReference<Map<String, Map<String, Object>>>() {
					});
			if (cachedUserDetails != null && cachedUserDetails.containsKey(username)) {
				logger.info("Fetching cached user details for username: {}", username);
				return cachedUserDetails.get(username);
			}
			if (cachedUserDetails == null) {
				cachedUserDetails = new HashMap<String, Map<String, Object>>();
			}
			Map<String, Object> userMap = new HashMap<>();
			userMap.put("username", username);
			users = userDAO.getUser(userMap, false);
			cachedUserDetails.put(username, users);
			cacheService.save(key, cachedUserDetails);
			logger.debug("User details cached for username: {}", username);
			logger.debug("Checking password match.");
			User user = (User) ((List<Object>) users.get("users")).get(0);
			if (!Helper.checkPassword(password, user.getPassword())) {
				logger.warn("Password mismatch for user: {}", username);
				throw new CustomException("Password does not match");
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
				userDAO.updateCustomer(columnCriteria, userMap);
			} else {
				logger.debug("Updating password for staff.");
				userDAO.updateStaff(columnCriteria, userMap);
			}
			cacheService.delete("userDetails");
			logger.info("Password updated successfully.");
			return true;
		} catch (CustomException e) {
			logger.error("Password update failed. Error: {}", e.getMessage());
			throw e;
		}
	}

	public <T extends User> Map<String, Object> getUserDetails(Map<String, Object> userMap, boolean notExact)
			throws CustomException {
		logger.info("Fetching user details.");
		try {
			return userDAO.getUser(userMap, notExact);
		} catch (CustomException e) {
			logger.error("Error fetching user details. Error: {}", e.getMessage());
			throw e;
		} catch (Exception e) {
			logger.error("Unexpected error occurred while fetching user details.", e);
			throw new CustomException("An unexpected error occurred while fetching user details.", e);
		}
	}

	public void createUser(Map<String, Object> userMap) throws CustomException {
		logger.info("Attempting to create user: {}", userMap);
		try {
			String role = (String) userMap.get("role");
			logger.debug("User role determined as: {}", role);
			if ("Customer".equals(role)) {
				logger.debug("Creating customer user.");
				userDAO.createCustomer(Helper.createPojoFromMap(userMap, dblayer.model.CustomerDetail.class));
			} else {
				logger.debug("Creating staff user.");
				userDAO.createStaff(Helper.createPojoFromMap(userMap, Staff.class));
			}
			logger.info("User created successfully.");
		} catch (CustomException e) {
			logger.error("Error creating user. User data: {}. Error: {}", userMap, e.getMessage());
			throw e;
		}
	}

	public void updateUserDetails(Map<String, Object> userMap) throws CustomException {
		logger.info("Attempting to update user details.");
		try {
			List<String> fields = new ArrayList<>();
			List<Object> values = new ArrayList<>();
			String role = (String) userMap.get("role");
			userMap.remove("role");
			logger.debug("User role determined as: {}", role);
			Map<String, Object> criteriaMap = new HashMap<>();
			for (String key : userMap.keySet()) {
				logger.debug("Processing field: {}", key);
				if (key.equals("userId")) {
					criteriaMap.put(key, userMap.get(key));
					continue;
				}
				fields.add(key);
				values.add(userMap.get(key));
			}

			ColumnCriteria columnCriteria = new ColumnCriteria();
			columnCriteria.setFields(fields);
			columnCriteria.setValues(values);
			
			if ("Customer".equals(role)) {
				logger.debug("Updating details for customer.");
				userDAO.updateCustomer(columnCriteria, criteriaMap);
			} else {
				logger.debug("Updating details for staff.");
				userDAO.updateStaff(columnCriteria, criteriaMap);
			}
			cacheService.delete("userDetails");
			logger.info("User details updated successfully.");
		} catch (CustomException e) {
			logger.error("Error updating user details. Error: {}", e.getMessage());
			throw e;
		}
	}

}