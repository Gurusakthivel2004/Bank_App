package dao;

import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.*;

import util.CustomException;
import util.Helper;
import util.SQLHelper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import Enum.Constants.HttpStatusCodes;

public class UserDAO {

	private static final Logger logger = LogManager.getLogger(UserDAO.class);

	public <T> void createUser(T user, String userType) throws CustomException {
		logger.info("Creating {}...", userType);
		try {
			Helper.checkNullValues(user);
			if (user instanceof CustomerDetail) {
				((CustomerDetail) user).setCreatedAt(System.currentTimeMillis())
						.setPerformedBy((long) Helper.getThreadLocalValue().get("id"))
						.setPassword(Helper.hashPassword(((CustomerDetail) user).getPassword()));
			} else if (user instanceof Staff) {
				((Staff) user).setCreatedAt(System.currentTimeMillis())
						.setPerformedBy((long) Helper.getThreadLocalValue().get("id"))
						.setPassword(Helper.hashPassword(((Staff) user).getPassword()));
			}
			SQLHelper.insert(user);
			logger.info("{} created successfully.", userType);
		} catch (Exception e) {
			logger.error("Unexpected error occurred while creating {}: {}", userType, e.getMessage());
			throw new CustomException("An error occurred while creating the " + userType + ". Please try later.",
					HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

	public <T> void updateUser(ColumnCriteria columnCriteria, Map<String, Object> userMap, Class<T> clazz)
			throws CustomException {
		logger.info("Updating {} with criteria: {}", clazz.getSimpleName(), columnCriteria);
		Helper.checkNullValues(columnCriteria);
		columnCriteria.getFields().add("modifiedAt");
		columnCriteria.getValues().add(System.currentTimeMillis());

		Criteria criteria = new Criteria().setClazz(clazz);
		applyUserFilters(criteria, userMap);

		try {
			SQLHelper.update(columnCriteria, criteria);
			logger.info("{} updated successfully.", clazz.getSimpleName());
		} catch (SQLException e) {
			logger.error("Error while updating {} details: ", clazz.getSimpleName(), e);
			throw new CustomException("Failed to update " + clazz.getSimpleName() + " details.",
					HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

	public <T extends MarkedClass> void removeUser(Class<T> clazz) throws CustomException {
		logger.info("Removing {}.", clazz.getSimpleName());
		ColumnCriteria columnCriteria = new ColumnCriteria().setFields(Arrays.asList("status"))
				.setValues(Arrays.asList("Inactive"));

		Criteria criteria = DAOHelper.buildCriteria(clazz, Arrays.asList("id"), Arrays.asList("EQUAL_TO"),
				Arrays.asList(Helper.getThreadLocalValue().get("id")));
		try {
			SQLHelper.update(columnCriteria, criteria);
			logger.info("{} removed successfully.", clazz.getSimpleName());
		} catch (SQLException e) {
			logger.error("Error while removing {}: ", clazz.getSimpleName(), e);
			throw new CustomException("Failed to remove " + clazz.getSimpleName() + ".",
					HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

	public <T extends MarkedClass> Map<String, Object> getUserDetails(Map<String, Object> userMap, Class<T> clazz,
			boolean notExact) throws CustomException {
		logger.info("Fetching {} details.", clazz.getSimpleName());
		Criteria criteria = buildUserCriteria(userMap, clazz, notExact);
		Map<String, Object> result = new HashMap<>();
		Long offset = (Long) userMap.getOrDefault("offset", -1L);
		try {
			if (offset == 0) {
				criteria.setOffsetValue(-1L).setAggregateFunction("COUNT").setAggregateOperator("*");
				result.put("count", SQLHelper.get(criteria).get(0));
			}
			if (offset >= 0) {
				criteria.setOffsetValue(offset);
			}
			result.put("userDetail", SQLHelper.get(criteria));
			return result;
		} catch (SQLException e) {
			logger.error("Error while fetching user details {}: ", clazz.getSimpleName(), e);
			throw new CustomException("Failed to fetch user details.", HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

	private <T extends MarkedClass> Criteria buildUserCriteria(Map<String, Object> userMap, Class<T> clazz,
			boolean notExact) {
		Criteria criteria = new Criteria();
		if (notExact) {
			Long userId = (Long) userMap.get("userId");
			criteria.setClazz(clazz).setSelectColumn(Arrays.asList("*")).setColumn(Arrays.asList("id", "id"))
					.setOperator(Arrays.asList("EQUAL_TO", "LIKE")).setValue(Arrays.asList(userId, "%" + userId + "%"))
					.setLimitValue(5).setLogicalOperator("OR");
		} else if (userMap.containsKey("role") && userMap.containsKey("userId")) {
			String role = (String) userMap.get("role");
			Long userId = (Long) userMap.get("userId");
			if (role.equals("Customer")) {
				criteria = DAOHelper.buildJoinCriteria(CustomerDetail.class, Arrays.asList("customer", "user"),
						Arrays.asList("customerDetail.user_id", "customer.user_id"),
						Arrays.asList("EQUAL_TO", "EQUAL_TO"), Arrays.asList("customer.user_id", "user.id"),
						Arrays.asList("customerDetail.user_id"), Arrays.asList("EQUAL_TO"), Arrays.asList(userId));
				criteria.setJoin(" JOIN ");
			} else {
				criteria = DAOHelper.buildJoinCriteria(Staff.class, Arrays.asList("user"),
						Arrays.asList("staff.user_id"), Arrays.asList("EQUAL_TO"), Arrays.asList("user.id"),
						Arrays.asList("staff.user_id"), Arrays.asList("EQUAL_TO"), Arrays.asList(userId));
				criteria.setJoin(" JOIN ");
			}
		} else {
			criteria = DAOHelper
					.buildCriteria(clazz, Arrays.asList("user.username"), Arrays.asList("EQUAL_TO"),
							Arrays.asList(userMap.get("username")))
					.setSelectColumn(Arrays.asList("user.*")).setColumn(new ArrayList<>()).setValue(new ArrayList<>())
					.setOperator(new ArrayList<>());
			criteria = applyBranchFilter(criteria, userMap);
			applyUserFilters(criteria, userMap);
		}
		if (userMap.containsKey("limit")) {
			criteria.setLimitValue(userMap.get("limit"));
		}
		return criteria;
	}

	private void applyUserFilters(Criteria criteria, Map<String, Object> userMap) {
		DAOHelper.addConditionIfPresent(criteria, userMap, "userId", "user.id", "EQUAL_TO", 0L);
		DAOHelper.addConditionIfPresent(criteria, userMap, "username", "user.username", "EQUAL_TO", "");
		DAOHelper.addConditionIfPresent(criteria, userMap, "role", "user.role", "EQUAL_TO", 0L);
		DAOHelper.addConditionIfPresent(criteria, userMap, "status", "user.status", "EQUAL_TO", "");
	}

	private Criteria applyBranchFilter(Criteria criteria, Map<String, Object> userMap) {
		if (!userMap.containsKey("branchId")) {
			return criteria;
		}
		List<String> joinTable = new ArrayList<>(Arrays.asList("account", "customer", "customerDetail", "staff"));
		criteria = DAOHelper
				.buildJoinCriteria(User.class, joinTable, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
						new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), " LEFT JOIN ", true)
				.setSelectColumn(Collections.singletonList("user.*"));
		DAOHelper.addJoinCondition(criteria, true, "account.user_id", "EQUAL_TO", "user.id");
		DAOHelper.addConditionIfPresent(criteria, userMap, "branchId", "account.branch_id", "EQUAL_TO", 0L);
		return criteria;
	}
}
