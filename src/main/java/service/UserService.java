package service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

import cache.CacheUtil;
import crm.ContactsService;
import dao.DAO;
import dao.DaoFactory;
import enums.Constants.ContactsFields;
import enums.Constants.Country;
import enums.Constants.HttpStatusCodes;
import enums.Constants.LogType;
import enums.Constants.Role;
import enums.Constants.TaskExecutor;
import model.ActivityLog;
import model.ColumnCriteria;
import model.CustomerDetail;
import model.Org;
import model.Staff;
import model.SubOrg;
import model.User;
import util.AuthUtils;
import util.CRMQueueManager;
import util.CustomException;
import util.Helper;
import util.ValidationUtil;

public class UserService {

	private Logger logger = LogManager.getLogger(UserService.class);
	private DAO<User> userDao = DaoFactory.getDAO(User.class);
	private DAO<CustomerDetail> customerDao = DaoFactory.getDAO(CustomerDetail.class);
	private DAO<Staff> staffDao = DaoFactory.getDAO(Staff.class);

	private UserService() {}

	private static class SingletonHelper {
		private static final UserService INSTANCE = new UserService();
	}

	public static UserService getInstance() {
		return SingletonHelper.INSTANCE;
	}

	private void validateUpdateRequest(Map<String, Object> userMap) throws CustomException {
		if (!userMap.containsKey("updatedValues") || !userMap.containsKey("userId")) {
			throw new CustomException("Provide the values to update", HttpStatusCodes.BAD_REQUEST);
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> extractUpdatedValues(Map<String, Object> userMap) {
		Map<String, Object> updatedValues = (Map<String, Object>) userMap.get("updatedValues");
		Helper.convertMapValuesToLong(updatedValues);
		userMap.remove("updatedValues");
		return updatedValues;
	}

	private Role extractRole(Map<String, Object> userMap) throws CustomException {
		Role role = Role.fromString((String) userMap.get("role"));
		userMap.remove("role");
		return role;
	}

	@SuppressWarnings("unchecked")
	private List<User> fetchExistingUsers(Map<String, Object> userMap) throws Exception {
		return (List<User>) getUserDetails(userMap).get("users");
	}

	private ColumnCriteria prepareUpdateCriteria(Map<String, Object> updatedValues) {
		List<String> fields = new ArrayList<>();
		List<Object> values = new ArrayList<>();

		updatedValues.forEach((key, value) -> {
			if (!"modifiedAt".equals(key) && !"performedBy".equals(key)) {
				fields.add(key);
				values.add(value);
			}
		});

		return new ColumnCriteria().setFields(fields).setValues(values);
	}

	private void updateUserInDatabase(Role role, ColumnCriteria columnCriteria, Map<String, Object> userMap)
			throws Exception {
		userMap.put("userClass", (role == Role.Customer) ? CustomerDetail.class : Staff.class);
		DAO<?> dao = (role == Role.Customer) ? customerDao : staffDao;
		dao.update(columnCriteria, userMap);
	}

	private void logUpdateActivity(Long userId, Map<String, Object> updatedValues) throws Exception {
		String logMessage = "Updated fields: " + String.join(", ", updatedValues.keySet());
		String logValues = "With values: " + updatedValues.values().toString();

		ActivityLog activityLog = new ActivityLog().setLogMessage(logMessage + " " + logValues)
				.setLogType(LogType.Update).setUserAccountNumber(null).setRowId(userId).setTableName("User")
				.setUserId(userId);

		Helper.logActivity(activityLog);
	}

	private void checkCaptchaRequirement(String username, HttpSession session) throws CustomException {
		if (AuthUtils.isCaptchaRequired(username, session)) {
			logger.warn("User {} must solve CAPTCHA before logging in", username);
			throw new CustomException("CAPTCHA verification required before login.", HttpStatusCodes.FORBIDDEN);
		}
	}

	private void checkUserLockout(String username) throws CustomException {
		if (AuthUtils.isUserLockedOut(username)) {
			logger.warn("User {} is temporarily locked out due to too many failed attempts", username);
			throw new CustomException("Too many failed attempts. Please wait 5 minutes and try again.",
					HttpStatusCodes.TOO_MANY_REQUESTS);
		}
	}

	private User validateCredentials(String username, String password) throws Exception {
		List<User> users = checkPassword(username, password);
		if (users == null || users.isEmpty() || users.size() > 1) {
			logger.warn("User not found or invalid credentials for username: {}", username);
			throw new CustomException("Invalid username or password.", HttpStatusCodes.UNAUTHORIZED);
		}
		return users.get(0);
	}

	private Map<String, Object> collectUserDetails(User user) {
		return Stream
				.of(new Object[][] { { "id", user.getId() }, { "fullname", user.getFullname() },
						{ "username", user.getUsername() }, { "status", user.getStatus() },
						{ "email", user.getEmail() }, { "phone", user.getPhone() }, { "role", user.getRole() } })
				.collect(Collectors.toMap(data -> (String) data[0], data -> data[1]));
	}

	private void logUserLoginActivity(User user) throws Exception {
		ActivityLog activityLog = new ActivityLog().setLogMessage("Login").setLogType(LogType.Login)
				.setUserAccountNumber(null).setRowId(user.getId()).setTableName("User").setUserId(user.getId())
				.setPerformedBy(user.getId());

		Helper.logActivity(activityLog);
	}

	private String getPasswordFromMap(Map<String, Object> passwordMap, String key) throws CustomException {
		String password = (String) passwordMap.get(key);
		if (password == null || password.isEmpty()) {
			throw new CustomException("Missing or empty password: " + key, HttpStatusCodes.BAD_REQUEST);
		}
		return password;
	}

	private void validateNewPassword(String newPassword) throws Exception {
		ValidationUtil.validatePassword(newPassword);
	}

	private void validateCurrentPassword(String currentPassword, User user) throws Exception {
		if (!Helper.checkPassword(user, currentPassword)) {
			logger.error("Password mismatch for user");
			throw new CustomException("Please enter the correct password", HttpStatusCodes.UNAUTHORIZED);
		}
	}

	private void clearUserCache() {
		CacheUtil.delete("userDetails");
	}

	private void logPasswordUpdateActivity(Long userId) throws Exception {
		ActivityLog activityLog = new ActivityLog().setLogMessage("Password updated").setLogType(LogType.Update)
				.setUserAccountNumber(null).setRowId(userId).setTableName("User").setUserId(userId);

		Helper.logActivity(activityLog);
	}

	private <T extends User> List<T> getCachedUsers(Map<String, Object> userMap) {
		String cacheKey = "userDetails";
		return CacheUtil.getCachedList(cacheKey, new TypeReference<List<T>>() {
		}, userMap, "userId");
	}

	private DAO<? extends User> determineDAO(Map<String, Object> userMap) throws CustomException {
		if (userMap.containsKey("userId") && userMap.containsKey("role")) {
			Role role = Role.fromString((String) userMap.get("role"));
			userMap.put("role", role);
			return (role == Role.Customer) ? customerDao : staffDao;
		}
		return userDao;
	}

	@SuppressWarnings("unchecked")
	private <T extends User> Class<T> determineUserClass(Map<String, Object> userMap) throws CustomException {
		if (userMap.containsKey("userId") && userMap.containsKey("role")) {
			Role role = (Role) userMap.get("role");
			return (role == Role.Customer) ? (Class<T>) CustomerDetail.class : (Class<T>) Staff.class;
		}
		return (Class<T>) User.class;
	}

	private long determineDataCount(Map<String, Object> userMap, DAO<?> dao) throws Exception {
		long offset = (long) userMap.getOrDefault("offset", -1l);
		return (offset == 0) ? dao.getDataCount(userMap) : -1;
	}

	private void cacheUserDataIfNeeded(Map<String, Object> userMap, List<?> users) {
		if (!userMap.containsKey("notExact") && userMap.containsKey("userId")) {
			CacheUtil.save("userDetails" + userMap.get("userId"), users);
		}
	}

	private <T extends User> Map<String, Object> createResultMap(List<T> users, Long count) {
		Map<String, Object> resultMap = new HashMap<>();
		resultMap.put("users", users);
		if (count != null) {
			resultMap.put("count", count);
		}
		return resultMap;
	}

	private void linkUserOrg(String orgName, String subOrgName, long userId) throws Exception {
		if (orgName.isEmpty() && subOrgName.isEmpty()) {
			return;
		}

		Map<String, Object> userMap = new HashMap<>();
		userMap.put("name", subOrgName);

		Org org = SubOrgService.getInstance().getParentOrgByKey("name", orgName);
		List<SubOrg> subOrgData = SubOrgService.getInstance().getSubOrgDetails(userMap);

		Long subOrgId = subOrgData.isEmpty() ? null : subOrgData.get(0).getId();
		SubOrgService.getInstance().createMembers(org.getId(), subOrgId, userId, Role.User);
	}

	private long createUserByRole(Map<String, Object> userMap, Role role) throws Exception {
		if (role == Role.Customer) {
			return handleCustomerCreation(userMap);
		} else {
			return handleStaffCreation(userMap);
		}
	}

	private long handleCustomerCreation(Map<String, Object> userMap) throws Exception {
		CustomerDetail customer = Helper.createPojoFromMap(userMap, CustomerDetail.class);
		ValidationUtil.validateCustomerModel(customer);
		long userId = userDao.create(customer);

		return userId;
	}

	private long handleStaffCreation(Map<String, Object> userMap) throws Exception {
		Staff staff = Helper.createPojoFromMap(userMap, Staff.class);
		ValidationUtil.validateStaffModel(staff);
		return userDao.create(staff);
	}

	private void logUserCreationActivity(long userId) {
		ActivityLog activityLog = new ActivityLog().setLogMessage("User created").setLogType(LogType.Insert)
				.setUserAccountNumber(null).setRowId(userId).setTableName("User").setUserId(userId);

		Helper.logActivity(activityLog);
	}

	private void updateContacts(Map<String, Object> userMap, Long userId, String criteriaEmail) throws Exception {

		Map<ContactsFields, Object> contactsMap = new HashMap<ContactsFields, Object>();

		if (userMap.containsKey("email")) {
			String email = (String) userMap.get("email");
			contactsMap.put(ContactsFields.EMAIL, email);
		}
		if (userMap.containsKey("phone")) {
			String phone = (String) userMap.get("phone");
			contactsMap.put(ContactsFields.PHONE,  phone);
		}
		if (contactsMap.size() == 0) {
			return;
		}

		TaskExecutor.CRM.submitTask(() -> {
			try {
				CRMQueueManager.addUpdateJsonToSortedSet(ContactsService.CRM_MODULE_PK, criteriaEmail, contactsMap,
						ContactsService.CRM_MODULE);
			} catch (Exception e) {
				logger.error("CRM Deals push failed: {}", e.getMessage(), e);
			}
		});

	}

	public Map<String, Object> userLogin(String username, String password, HttpSession session) throws Exception {
		logger.info("Attempting login for username: {}", username);

		checkUserLockout(username);
		checkCaptchaRequirement(username, session);

		User user = validateCredentials(username, password);
		Map<String, Object> userDetails = collectUserDetails(user);

		if (Role.Customer != user.getRoleEnum()) {
			addStaffDetails(userDetails, user);
		}
		logUserLoginActivity(user);
		return userDetails;
	}

	public User getUserById(Long userId) throws Exception {
		String key = "userId:" + userId;
		List<User> cachedUsers = CacheUtil.getCachedList(key, new TypeReference<List<User>>() {
		});
		if (cachedUsers != null) {
			return cachedUsers.get(0);
		}

		Map<String, Object> userQuery = new HashMap<>();
		userQuery.put("userId", userId);
		userQuery.put("password", true);
		userQuery.put("userClass", User.class);

		List<User> users = userDao.get(userQuery);
		if (users == null || users.isEmpty()) {
			throw new CustomException("User not found", HttpStatusCodes.NOT_FOUND);
		}

		CacheUtil.save(key, users);
		return users.get(0);
	}

	public void addStaffDetails(Map<String, Object> userDetails, User user) throws CustomException {
		logger.info("Fetching additional staff details for employee user.");
		try {
			String key = "staffId:" + user.getId();
			Map<String, Object> userMap = new HashMap<>();
			userMap.put("userId", user.getId());
			userMap.put("role", user.getRoleEnum());
			userMap.put("userClass", Staff.class);

			List<Staff> staffDetails = CacheUtil.getCachedList(key, new TypeReference<List<Staff>>() {
			});
			if (staffDetails == null) {
				staffDetails = staffDao.get(userMap);
				CacheUtil.save(key, staffDetails);
			}

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

	private List<User> checkPassword(String username, String password) throws Exception {
		logger.info("Validating password for user: {}", username);
		try {
			List<User> users = null;

			Map<String, Object> userMap = new HashMap<>();
			userMap.put("username", username);
			userMap.put("password", true);
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
			if (!Helper.checkPassword(user, password)) {
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

	public void updatePassword(Map<String, Object> passwordMap) throws Exception {
		logger.info("Attempting to update password.");

		try {
			long userId = (long) Helper.getThreadLocalValue("id");
			String currentPassword = getPasswordFromMap(passwordMap, "currentPassword");
			String newPassword = getPasswordFromMap(passwordMap, "newPassword");

			validateNewPassword(newPassword);
			User user = getUserById(userId);
			validateCurrentPassword(currentPassword, user);

			updateUserPassword(userId, newPassword, user.getRole());
			clearUserCache();

			logPasswordUpdateActivity(userId);

			logger.info("Password updated successfully.");
		} catch (CustomException e) {
			logger.error("Password update failed. Error: {}", e.getMessage());
			throw e;
		}
	}

	public void updateUserPassword(Long userId, String newPassword, String userRole) throws Exception {
		ColumnCriteria columnCriteria = new ColumnCriteria()
				.setFields(new ArrayList<>(Arrays.asList("password", "passwordVersion")))
				.setValues(new ArrayList<>(Arrays.asList(Helper.hashPassword(newPassword, 1), 1)));

		Role role = Role.fromString(userRole);
		Class<?> userClass = (role == Role.Customer) ? CustomerDetail.class : Staff.class;

		Map<String, Object> updateQuery = new HashMap<>();
		updateQuery.put("userId", userId);
		updateQuery.put("userClass", userClass);

		userDao.update(columnCriteria, updateQuery);
	}

	public <T extends User> Map<String, Object> getUserDetails(Map<String, Object> userMap) throws Exception {
		logger.info("Fetching user details.");

		List<T> cachedUsers = getCachedUsers(userMap);
		if (cachedUsers != null && userMap.size() == 1) {
			return createResultMap(cachedUsers, null);
		}

		DAO<? extends User> dao = determineDAO(userMap);
		Class<T> clazz = determineUserClass(userMap);

		userMap.put("userClass", clazz);
		long count = determineDataCount(userMap, dao);

		List<? extends User> users = dao.get(userMap);
		cacheUserDataIfNeeded(userMap, users);

		return createResultMap(users, count);

	}

	public void createUser(Map<String, Object> userMap) throws Exception {
		logger.info("Attempting to create user");

		logger.info("userMap keys: {}", userMap.keySet());
		logger.info("userMap values: {}", userMap.values());

		String orgName = (String) userMap.remove("orgName");
		String subOrgName = (String) userMap.remove("subOrgName");

		String countryName = (String) userMap.remove("country");
		Country country = Country.fromCountryName(countryName);

		Role role = Role.fromString((String) userMap.get("role"));
		userMap.put("status", "Active");
		userMap.put("countryCode", country.getRegionCode());

		try {
			long userId = createUserByRole(userMap, role);
			linkUserOrg(orgName, subOrgName, userId);

			logUserCreationActivity(userId);
			logger.info("User created successfully.");
		} catch (CustomException e) {
			logger.error("Error creating user. User data: {}. Error: {}", userMap, e);
			throw e;
		}
	}

	public void updateUserDetails(Map<String, Object> userMap) throws Exception {
		logger.info("Attempting to update user details.");

		try {
			if (userMap.containsKey("currentPassword")) {
				updatePassword(userMap);
				return;
			}

			logger.info(userMap);
			validateUpdateRequest(userMap);

			long userId = Helper.parseLong(userMap.getOrDefault("userId", -1));
			Map<String, Object> updatedValues = extractUpdatedValues(userMap);

			Role role = extractRole(userMap);
			Class<? extends User> clazz = (role == Role.Customer) ? CustomerDetail.class : Staff.class;
			ValidationUtil.validateUpdateFields(updatedValues, clazz);
			List<User> users = fetchExistingUsers(userMap);
			if (users == null || users.isEmpty()) {
				throw new CustomException("User not found.", HttpStatusCodes.BAD_REQUEST);
			}

			ColumnCriteria columnCriteria = prepareUpdateCriteria(updatedValues);
			updateUserInDatabase(role, columnCriteria, userMap);

			CacheUtil.delete("userDetails");
			logUpdateActivity(userId, updatedValues);

			updateContacts(updatedValues, users.get(0).getId(), users.get(0).getEmail());
		} catch (CustomException e) {
			logger.error("Error updating user details. Error: {}", e.getMessage());
			throw e;
		}
	}

}