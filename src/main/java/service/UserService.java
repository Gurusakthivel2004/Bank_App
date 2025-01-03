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

	/**
	 * Handles user login.
	 *
	 * @param username the username of the user.
	 * @param password the password of the user.
	 * @return a map containing user details.
	 * @throws CustomException if the username or password is invalid.
	 */
	public Map<String, Object> userLogin(String username, String password) throws CustomException {
		logger.info("Attempting login for username: {}", username);

		try {
			List<Object> users = checkPassword(username, password);
			if (users == null || users.isEmpty() || users.size() > 1) {
				logger.warn("User not found or invalid credentials for username: {}", username);
				throw new CustomException("Invalid username or password.");
			}
			User user = (User) users.get(0);
			Map<String, Object> userDetails = Stream
					.of(new Object[][] { { "id", user.getId() }, { "fullname", user.getFullname() },
							{ "username", user.getUsername() }, { "status", user.getStatus() },
							{ "email", user.getEmail() }, { "phone", user.getPhone() }, { "role", user.getRole() }, })
					.collect(Collectors.toMap(data -> (String) data[0], data -> data[1]));

			// Add additional staff details for employee role
			if (!"Customer".equals(user.getRole())) {
				logger.debug("Fetching additional staff details for employee user.");
				List<Object> staffDetails = userDAO.getStaff(user.getId());
				if (!staffDetails.isEmpty()) {
					userDetails.put("branchId", ((Staff) staffDetails.get(0)).getBranchId());
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
			logger.error("Unexpected error occurred during login for username: {}. Error: {}", username, e);
			throw new CustomException("An unexpected error occurred. Please try again later.", e);
		}
	}

	/**
	 * Validates the password for a user and returns user object.
	 *
	 * @param column   the column to search by (e.g., username, email).
	 * @param value    the value of the column.
	 * @param password the password to validate.
	 * @return the User object if validation succeeds.
	 * @throws CustomException if the password is invalid.
	 */
	private List<Object> checkPassword(String username, String password) throws CustomException {
		logger.info("Validating password for user: {}", username);
		try {
			logger.debug("Fetching user for column: username and value: {}", username);
			String key = "userDetails";
			List<Object> users = null;
			Map<String, List<Object>> cachedUserDetails = cacheService.get(key,
					new TypeReference<Map<String, List<Object>>>() {
					});
			if (cachedUserDetails != null && cachedUserDetails.containsKey(username)) {
				logger.info("Fetching cached user details for username: {}", username);
				users = cachedUserDetails.get(username);
			} else {
				if (cachedUserDetails == null) {
					cachedUserDetails = new HashMap<String, List<Object>>();
				}
				users = userDAO.getUser(Arrays.asList("username"), username, false);
				cachedUserDetails.put(username, users);
				cacheService.save(key, cachedUserDetails);
				logger.debug("User details cached for username: {}", username);
			}
			logger.debug("Checking password match.");
			System.out.println("users: " + users);
			User user = (User) users.get(0);
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

	/**
	 * Updates the user's password.
	 *
	 * @param currentPassword the current password of the user.
	 * @param newPassword     the new password to set.
	 * @return true if the password is updated successfully.
	 * @throws CustomException if the update fails.
	 */
	public boolean updatePassword(String currentPassword, String newPassword) throws CustomException {
		logger.info("Attempting to update password.");
		try {
			ColumnCriteria columnCriteria = new ColumnCriteria();
			logger.debug("Hashing new password.");
			columnCriteria.setFields(new ArrayList<>(Arrays.asList("password")));
			columnCriteria.setValues(new ArrayList<>(Arrays.asList(Helper.hashPassword(newPassword))));

			String role = (String) Helper.getThreadLocalValue().get("role");
			logger.debug("Determining update type based on role: {}", role);
			if ("Customer".equals(role)) {
				logger.debug("Updating password for customer.");
				userDAO.updateCustomer(columnCriteria);
			} else {
				logger.debug("Updating password for staff.");
				userDAO.updateStaff(columnCriteria);
			}
			cacheService.delete("userDetails");
			logger.info("Password updated successfully.");
			return true;
		} catch (CustomException e) {
			logger.error("Password update failed. Error: {}", e.getMessage());
			throw e;
		}
	}

	/**
	 * Retrieves user details based on the role.
	 * 
	 * @param userId id of the user.
	 * @return user details as an Object.
	 * @throws CustomException if retrieval fails.
	 */
	public <T extends User> List<Object> getUserDetails(Long userId, String role, boolean notExact)
			throws CustomException {
		logger.info("Fetching user details.");
		try {
			String key = role + "Details of id: " + userId;

			logger.debug("User role determined as: {}", role);

			Map<Long, List<Object>> cachedUserDetails = cacheService.get(key,
					new TypeReference<Map<Long, List<Object>>>() {
					});

			if (cachedUserDetails != null && cachedUserDetails.containsKey(userId)) {
				logger.info("Fetching cached user details for userId: {}", userId);
				return (List<Object>) cachedUserDetails.get(userId);
			} else {
				if (cachedUserDetails == null) {
					cachedUserDetails = new HashMap<>();
				}
				List<Object> user;
				if (notExact) {
					logger.info("Fetching details for customer.");
					user = userDAO.getUser(Arrays.asList("id", "fullname"), userId, notExact);
				} else if ("Customer".equals(role)) {
					logger.info("Fetching details for customer.");
					user = userDAO.getCustomers(userId);
				} else {
					logger.info("Fetching details for staff.");
					user = userDAO.getStaff(userId);
				}
				cachedUserDetails.put(userId, user);
				cacheService.save(key, cachedUserDetails);
				logger.debug("{} details cached for userId: {}", role, userId);

				return user;
			}
		} catch (CustomException e) {
			logger.error("Error fetching user details. Error: {}", e.getMessage());
			throw e;
		} catch (Exception e) {
			logger.error("Unexpected error occurred while fetching user details. Error: {}", e.getMessage());
			throw new CustomException("An unexpected error occurred while fetching user details.", e);
		}
	}

	/**
	 * Creates a new user based on the provided map.
	 *
	 * @param userMap a map containing user details.
	 * @throws CustomException if creation fails.
	 */
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

	/**
	 * Updates user details based on the provided map.
	 *
	 * @param userMap a map containing updated user details.
	 * @throws CustomException if the update fails.
	 */
	public void updateUserDetails(Map<String, Object> userMap) throws CustomException {
		logger.info("Attempting to update user details.");
		try {
			List<String> fields = new ArrayList<>();
			List<Object> values = new ArrayList<>();
			String role = (String) Helper.getThreadLocalValue().get("role");
			logger.debug("User role determined as: {}", role);

			for (String key : userMap.keySet()) {
				logger.debug("Processing field: {}", key);
				fields.add(key);
				values.add(userMap.get(key));
			}

			ColumnCriteria columnCriteria = new ColumnCriteria();
			columnCriteria.setFields(fields);
			columnCriteria.setValues(values);

			if ("Customer".equals(role)) {
				logger.debug("Updating details for customer.");
				userDAO.updateCustomer(columnCriteria);
			} else {
				logger.debug("Updating details for staff.");
				userDAO.updateStaff(columnCriteria);
			}
			cacheService.delete("userDetails");
			logger.info("User details updated successfully.");
		} catch (CustomException e) {
			logger.error("Error updating user details. Error: {}", e.getMessage());
			throw e;
		}
	}

}