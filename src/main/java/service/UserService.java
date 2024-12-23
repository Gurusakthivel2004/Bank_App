package service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator.OfDouble;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import dblayer.dao.UserDAO;
import dblayer.model.ColumnCriteria;
import dblayer.model.Staff;
import dblayer.model.User;
import util.CustomException;
import util.Helper;

public class UserService {

    private static final Logger logger = LogManager.getLogger(UserService.class);
    private static UserDAO userDAO = new UserDAO();

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
            logger.debug("Fetching user details for validation.");
            Map<String, Object> userDetails = new HashMap<>();
            User user = checkPassword("username", username, password);

            logger.debug("Populating user details for the response.");
            userDetails.put("id", user.getId());
            userDetails.put("fullname", user.getFullname());
            userDetails.put("status", user.getStatus());
            userDetails.put("email", user.getEmail());
            userDetails.put("phone", user.getPhone());
            userDetails.put("role", user.getRole());
            if ("Employee".equals(user.getRole())) {
                logger.debug("Fetching additional staff details for employee user.");
                List<Staff> staffDetails = userDAO.getStaff(user.getId());
                userDetails.put("branchId", staffDetails.get(0).getBranchId());
            }
            logger.info("User login successful for username and password: {}", username, password);
            return userDetails;
        } catch (CustomException e) {
            logger.error("User login failed for username: {}. Error: {}", username, e.getMessage());
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

            logger.info("Password updated successfully.");
            return true;
        } catch (CustomException e) {
            logger.error("Password update failed. Error: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Retrieves user details based on the role.
     * @param userId id of the user.
     * @return user details as an Object.
     * @throws CustomException if retrieval fails.
     */
    public Object getUserDetails(Long userId) throws CustomException {
        logger.info("Fetching user details.");
        try {
            String role = (String) Helper.getThreadLocalValue().get("role");
            logger.debug("User role determined as: {}", role);

            if ("Customer".equals(role)) {
                logger.info("Fetching details for customer.");
                return userDAO.getCustomers(userId).get(0);
            } else {
                logger.info("Fetching details for staff.");
                return userDAO.getStaff(userId).get(0);
            }
        } catch (CustomException e) {
            logger.error("Error fetching user details. Error: {}", e.getMessage());
            throw e;
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

            logger.info("User details updated successfully.");
        } catch (CustomException e) {
            logger.error("Error updating user details. Error: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Validates the password for a user.
     *
     * @param column   the column to search by (e.g., username, email).
     * @param value    the value of the column.
     * @param password the password to validate.
     * @return the User object if validation succeeds.
     * @throws CustomException if the password is invalid.
     */
    private User checkPassword(String column, Object value, String password) throws CustomException {
        logger.info("Validating password for user: {}", value);
        try {
            logger.debug("Fetching user for column: {} and value: {}", column, value);
            System.out.println(column + " " + value + " " + password);
            User user = userDAO.getUser(column, value);
            logger.debug("Checking password match.");
            if (!Helper.checkPassword(password, user.getPassword())) {
                logger.warn("Password mismatch for user: {}", value);
                throw new CustomException("Password does not match");
            }

            logger.info("Password validation successful for user: {}", value);
            return user;
        } catch (CustomException e) {
            logger.error("Password validation failed for user: {}. Error: {}", value, e.getMessage());
            throw e;
        }
    }
}
