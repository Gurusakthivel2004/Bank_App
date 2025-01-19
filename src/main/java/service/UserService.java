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
import cache.CacheUtil;
import dao.DAO;
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

			if (!"Customer".equals(user.getRole())) {
				addStaffDetails(userDetails, user);
			}

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
			userMap.put("role", user.getRole());
			userMap.put("userClass", Staff.class);
			userMap.put("notExact", false);

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
			userMap.put("notExact", false);
			users = userDao.get(userMap);

			User user = users.get(0);

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

	public boolean updatePassword(String currentPassword, String newPassword) throws CustomException {
		logger.info("Attempting to update password.");
		try {
			ColumnCriteria columnCriteria = new ColumnCriteria().setFields(Arrays.asList("password"))
					.setValues(Arrays.asList(Helper.hashPassword(newPassword)));

			String role = (String) Helper.getThreadLocalValue("role");
			Map<String, Object> userMap = new HashMap<>();
			userMap.put("userId", Helper.getThreadLocalValue("id"));
			if ("Customer".equals(role)) {
				userMap.put("userClass", CustomerDetail.class);
				userDao.update(columnCriteria, userMap);
			} else {
				userMap.put("userClass", Staff.class);
				userDao.update(columnCriteria, userMap);
			}
			cacheUtil.delete("userDetails");
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
			String key = "userDetails";
			List<Object> cachedUsers = cacheUtil.getCachedList(key, new TypeReference<List<Object>>() {
			}, userMap, "userId");

			if (cachedUsers != null) {
				return createResultMap(cachedUsers, null);
			}

			DAO<?> dao = userDao;
			Class<T> clazz = (Class<T>) User.class;

			if (userMap.containsKey("userId") && userMap.containsKey("role")) {
				String role = (String) userMap.get("role");
				if (role.equals("Customer")) {
					dao = customerDao;
					clazz = (Class<T>) CustomerDetail.class;
				} else {
					dao = staffDao;
					clazz = (Class<T>) Staff.class;
				}
			}

			userMap.put("userClass", clazz);
			userMap.put("notExact", notExact);

			Long offset = (Long) userMap.getOrDefault("offset", -1L);
			Long count = (offset == 0) ? dao.getDataCount(userMap) : null;

			List<?> users = dao.get(userMap);

			if (!notExact && userMap.containsKey("userId") && userMap.containsKey("role")) {
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
			String role = (String) userMap.get("role");
			if ("Customer".equals(role)) {
				CustomerDetail customerDetail = Helper.createPojoFromMap(userMap, CustomerDetail.class);
				userDao.create(customerDetail);
			} else {
				Staff staff = Helper.createPojoFromMap(userMap, Staff.class);
				userDao.create(staff);
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

			for (String key : updatedValues.keySet()) {
				fields.add(key);
				values.add(updatedValues.get(key));
			}
			userMap.remove("updatedValues");

			ColumnCriteria columnCriteria = new ColumnCriteria().setFields(fields).setValues(values);
			if ("Customer".equals(role)) {
				userMap.put("userClass", CustomerDetail.class);
				customerDao.update(columnCriteria, userMap);
			} else {
				userMap.put("userClass", Staff.class);
				staffDao.update(columnCriteria, userMap);
			}
			cacheUtil.delete("userDetails");
			logger.info("User details updated successfully.");
		} catch (CustomException e) {
			logger.error("Error updating user details. Error: {}", e.getMessage());
			throw e;
		}
	}

}