package dao;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
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

public class UserDAO {
	private static final Logger logger = LogManager.getLogger(UserDAO.class);

	public Map<String, Object> getUser(Map<String, Object> userMap, boolean notExact) throws CustomException {

		if (userMap.containsKey("userId") && userMap.containsKey("role")) {
			String role = (String) userMap.get("role");
			if (role.equals("Customer")) {
				return getCustomers((Long) userMap.get("userId"));
			} else {
				return getStaff((Long) userMap.get("userId"));
			}
		}
		Criteria criteria = new Criteria();
		if (notExact) {
			Long userId = (Long) userMap.get("userId");
			criteria.setClazz(User.class).setSelectColumn(Arrays.asList("*")).setColumn(Arrays.asList("id", "id"))
					.setOperator(Arrays.asList("EQUAL_TO", "LIKE")).setValue(Arrays.asList(userId, "%" + userId + "%"))
					.setLimitValue(5).setLogicalOperator("OR");
		} else {
			criteria = DAOHelper
					.buildCriteria(User.class, Arrays.asList("username"), Arrays.asList("EQUAL_TO"),
							Arrays.asList(userMap.get("username")))
					.setSelectColumn(Arrays.asList("user.*")).setColumn(new ArrayList<>()).setValue(new ArrayList<>())
					.setOperator(new ArrayList<>());
			criteria = applyBranchFilter(criteria, userMap);
			applyUserFilters(criteria, userMap);
		}

		if (userMap.containsKey("limit")) {
			criteria.setLimitValue(userMap.get("limit"));
		}
		Map<String, Object> result = new HashMap<>();

		Long offset = (Long) userMap.getOrDefault("offset", -1L);
		if (offset == 0) {
			criteria.setOffsetValue(-1L).setAggregateFunction("COUNT").setAggregateOperator("*");
			result.put("count", SQLHelper.get(criteria).get(0));
		}
		if (offset >= 0) {
			criteria.setOffsetValue(offset);
		}
		result.put("users", SQLHelper.get(criteria));
		return result;
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
		DAOHelper.addConditionIfPresent(criteria, userMap, "branchId", "account.branch_id", "EQUAL_TO", 0l);
		return criteria;
	}

	private void applyUserFilters(Criteria criteria, Map<String, Object> userMap) {
		DAOHelper.addConditionIfPresent(criteria, userMap, "userId", "user.id", "EQUAL_TO", 0L);
		DAOHelper.addConditionIfPresent(criteria, userMap, "username", "user.username", "EQUAL_TO", "");
		DAOHelper.addConditionIfPresent(criteria, userMap, "role", "user.role", "EQUAL_TO", 0L);
		DAOHelper.addConditionIfPresent(criteria, userMap, "status", "user.status", "EQUAL_TO", "");
	}

	public void createCustomer(CustomerDetail customer) throws CustomException {
		logger.info("Creating customer...");
		try {
			Helper.checkNullValues(customer);
			customer.setCreatedAt(System.currentTimeMillis())
					.setPerformedBy((long) Helper.getThreadLocalValue().get("id"))
					.setPassword(Helper.hashPassword(customer.getPassword()));

			SQLHelper.insert(customer);
			logger.info("Customer created successfully.");
		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			logger.error("Unexpected error occurred: {}", e.getMessage());
			throw new CustomException("An error occurred while creating the customer. Please try later.");
		}
	}

	public void updateCustomer(ColumnCriteria columnCriteria, Map<String, Object> userMap) throws CustomException {
		logger.info("Updating customer with criteria: {}", columnCriteria);
		Helper.checkNullValues(columnCriteria);
		columnCriteria.getFields().add("modifiedAt");
		columnCriteria.getValues().add(System.currentTimeMillis());

		Criteria criteria = new Criteria().setClazz(CustomerDetail.class);
		applyUserFilters(criteria, userMap);
		SQLHelper.update(columnCriteria, criteria);
		logger.info("Customer updated successfully.");
	}

	public Map<String, Object> getCustomers(Long customerId) throws CustomException {
		logger.info("Fetching customers.");
		Criteria customerJoinCriteria = DAOHelper.buildJoinCriteria(CustomerDetail.class,
				Arrays.asList("customer", "user"), Arrays.asList("customerDetail.user_id", "customer.user_id"),
				Arrays.asList("EQUAL_TO", "EQUAL_TO"), Arrays.asList("customer.user_id", "user.id"),
				Arrays.asList("customerDetail.user_id"), Arrays.asList("EQUAL_TO"), Arrays.asList(customerId));
		customerJoinCriteria.setJoin(" JOIN ");
		List<Object> customers = SQLHelper.get(customerJoinCriteria);

		logger.info("Fetched {} customers successfully.", customers.size());
		Map<String, Object> resultMap = new HashMap<String, Object>();
		resultMap.put("customerDetail", customers);
		return resultMap;

	}

	public void removeCustomer() throws CustomException {
		logger.info("Removing customer.");
		ColumnCriteria columnCriteria = new ColumnCriteria().setFields(Arrays.asList("status"))
				.setValues(Arrays.asList("Inactive"));

		Criteria customerCriteria = DAOHelper.buildCriteria(CustomerDetail.class, Arrays.asList("id"),
				Arrays.asList("EQUAL_TO"), Arrays.asList(Helper.getThreadLocalValue().get("id")));
		SQLHelper.update(columnCriteria, customerCriteria);

		customerCriteria.setClazz(Account.class);
		customerCriteria.getColumn().set(0, "customer_id");
		SQLHelper.update(columnCriteria, customerCriteria);
		logger.info("Customer removed successfully.");
	}

	public void createStaff(Staff staff) throws CustomException {
		logger.info("Creating staff: {}", staff);
		try {
			Helper.checkNullValues(staff);
			staff.setCreatedAt(System.currentTimeMillis()).setPerformedBy((long) Helper.getThreadLocalValue().get("id"))
					.setPassword(Helper.hashPassword(staff.getPassword()));
			SQLHelper.insert(staff);
			logger.info("Staff created successfully.");
		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			logger.error("Unexpected error occurred: {}", e.getMessage());
			throw new CustomException("An error occurred while creating the customer. Please try later.");
		}
	}

	public void updateStaff(ColumnCriteria columnCriteria, Map<String, Object> userMap) throws CustomException {
		logger.info("Updating staff with criteria: {}", columnCriteria);
		Helper.checkNullValues(columnCriteria);
		columnCriteria.getFields().addAll(Arrays.asList("modified_at", "performed_by"));
		columnCriteria.getValues()
				.addAll(Arrays.asList(System.currentTimeMillis(), Helper.getThreadLocalValue().get("id")));

		Criteria criteria = new Criteria().setClazz(Staff.class);

		applyUserFilters(criteria, userMap);
		SQLHelper.update(columnCriteria, criteria);
		logger.info("Staff updated successfully.");
	}

	public Map<String, Object> getStaff(Long id) throws CustomException {
		if (id == -1) {
			id = (Long) Helper.getThreadLocalValue().get("id");
		}

		logger.info("Fetching staff with id: {}", id);
		Criteria staffJoinCriteria = DAOHelper.buildJoinCriteria(Staff.class, Arrays.asList("user"),
				Arrays.asList("staff.user_id"), Arrays.asList("EQUAL_TO"), Arrays.asList("user.id"),
				Arrays.asList("staff.user_id"), Arrays.asList("EQUAL_TO"), Arrays.asList(id));
		staffJoinCriteria.setJoin(" JOIN ");
		List<Object> staffList = SQLHelper.get(staffJoinCriteria);
		logger.info("Fetched {} staff members successfully.", staffList.size());
		Map<String, Object> resultMap = new HashMap<String, Object>();
		resultMap.put("staff", staffList);
		return resultMap;
	}

	public void removeStaff() throws CustomException {
		logger.info("Removing staff.");
		ColumnCriteria columnCriteria = new ColumnCriteria().setFields(Arrays.asList("status"))
				.setValues(Arrays.asList("Inactive"));

		Criteria staffCriteria = DAOHelper.buildCriteria(Staff.class, Arrays.asList("user_id"),
				Arrays.asList("EQUAL_TO"), Arrays.asList(Helper.getThreadLocalValue().get("id")));
		SQLHelper.update(columnCriteria, staffCriteria);
		logger.info("Staff removed successfully.");
	}
}
