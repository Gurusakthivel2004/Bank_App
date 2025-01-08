package dblayer.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import dblayer.model.*;
import util.CustomException;
import util.Helper;
import util.SQLHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UserDAO {
	private static final Logger logger = LogManager.getLogger(UserDAO.class);

	public Map<String, Object> getUser(Map<String, Object> userMap, boolean notExact) throws CustomException {
		System.out.println(userMap.keySet());
		System.out.println(userMap.values());
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
			criteria.setClazz(User.class);
			criteria.setSelectColumn(Arrays.asList("*"));
			criteria.setColumn(Arrays.asList("id", "id"));
			criteria.setOperator(Arrays.asList("=", "LIKE"));
			criteria.setValue(Arrays.asList(userId, "%" + userId + "%"));
			criteria.setLimitValue(5);
			criteria.setLogicalOperator("OR");
		} else {
			criteria = Helper.buildCriteria(User.class, Arrays.asList("username"), Arrays.asList("="),
					Arrays.asList(userMap.get("username")));
			criteria.setSelectColumn(Arrays.asList("user.*"));
			criteria.setColumn(new ArrayList<>());
			criteria.setValue(new ArrayList<>());
			criteria.setOperator(new ArrayList<>());
			criteria = applyBranchFilter(criteria, userMap);
			applyUserFilters(criteria, userMap);
		}

		if (userMap.containsKey("limit")) {
			criteria.setLimitValue(userMap.get("limit"));
		}
		Map<String, Object> result = new HashMap<>();

		Long offset = (Long) userMap.getOrDefault("offset", -1L);
		if (offset == 0) {
			criteria.setOffsetValue(-1L);
			result.put("count", SQLHelper.get(criteria).get(0));
		}
		if (offset >= 0) {
			criteria.setOffsetValue(offset);
		}
		System.out.println(criteria);
		result.put("users", SQLHelper.get(criteria));
		return result;
	}

	private Criteria applyBranchFilter(Criteria criteria, Map<String, Object> userMap) {
		if (!userMap.containsKey("branchId")) {
			return criteria;
		}
		List<String> joinTable = new ArrayList<>(Arrays.asList("account", "customer", "customerDetail", "staff"));
		criteria = Helper.buildJoinCriteria(User.class, joinTable, new ArrayList<>(), new ArrayList<>(),
				new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), " LEFT JOIN ", true);
		criteria.setSelectColumn(Collections.singletonList("user.*"));
		Helper.addJoinCondition(criteria, true, "account.user_id", "=", "user.id");
		Helper.addConditionIfPresent(criteria, userMap, "branchId", "account.branch_id", "=", 0l);
		return criteria;
	}

	private void applyUserFilters(Criteria criteria, Map<String, Object> userMap) {
		Helper.addConditionIfPresent(criteria, userMap, "userId", "user.id", "=", 0L);
		Helper.addConditionIfPresent(criteria, userMap, "username", "user.username", "=", "");
		Helper.addConditionIfPresent(criteria, userMap, "role", "user.role", "=", 0L);
		Helper.addConditionIfPresent(criteria, userMap, "status", "user.status", "=", "");
	}

	public void createCustomer(CustomerDetail customer) throws CustomException {
		logger.info("Creating customer: {}", customer);
		Helper.checkNullValues(customer);
		customer.setCreatedAt(System.currentTimeMillis());
		customer.setPerformedBy((long) Helper.getThreadLocalValue().get("id"));
		customer.setPassword(Helper.hashPassword(customer.getPassword()));
		SQLHelper.insert(customer);
		logger.info("Customer created successfully.");
	}

	public void updateCustomer(ColumnCriteria columnCriteria, Map<String, Object> userMap) throws CustomException {
		logger.info("Updating customer with criteria: {}", columnCriteria);
		Helper.checkNullValues(columnCriteria);
		columnCriteria.getFields().add("modifiedAt");
		columnCriteria.getValues().add(System.currentTimeMillis());
		
		Criteria criteria = new Criteria();
		criteria.setClazz(CustomerDetail.class);
		applyUserFilters(criteria, userMap);
		SQLHelper.update(columnCriteria, criteria);
		logger.info("Customer updated successfully.");
	}

	public Map<String, Object> getCustomers(Long customerId) throws CustomException {
		logger.info("Fetching customers.");
		Criteria customerJoinCriteria = Helper.buildJoinCriteria(CustomerDetail.class,
				Arrays.asList("customer", "user"), Arrays.asList("customerDetail.user_id", "customer.user_id"),
				Arrays.asList("=", "="), Arrays.asList("customer.user_id", "user.id"),
				Arrays.asList("customerDetail.user_id"), Arrays.asList("="), Arrays.asList(customerId));
		customerJoinCriteria.setJoin(" JOIN ");
		List<Object> customers = SQLHelper.get(customerJoinCriteria);

		logger.info("Fetched {} customers successfully.", customers.size());
		Map<String, Object> resultMap = new HashMap<String, Object>();
		resultMap.put("customerDetail", customers);
		return resultMap;

	}

	public void removeCustomer() throws CustomException {
		logger.info("Removing customer.");
		ColumnCriteria columnCriteria = new ColumnCriteria();
		columnCriteria.setFields(Arrays.asList("status"));
		columnCriteria.setValues(Arrays.asList("Inactive"));

		Criteria customerCriteria = Helper.buildCriteria(CustomerDetail.class, Arrays.asList("id"), Arrays.asList("="),
				Arrays.asList(Helper.getThreadLocalValue().get("id")));
		SQLHelper.update(columnCriteria, customerCriteria);

		customerCriteria.setClazz(Account.class);
		customerCriteria.getColumn().set(0, "customer_id");
		SQLHelper.update(columnCriteria, customerCriteria);
		logger.info("Customer removed successfully.");
	}

	public void createStaff(Staff staff) throws CustomException {
		logger.info("Creating staff: {}", staff);
		Helper.checkNullValues(staff);
		staff.setCreatedAt(System.currentTimeMillis());
		staff.setPerformedBy((long) Helper.getThreadLocalValue().get("id"));
		staff.setPassword(Helper.hashPassword(staff.getPassword()));
		SQLHelper.insert(staff);
		logger.info("Staff created successfully.");
	}

	public void updateStaff(ColumnCriteria columnCriteria, Map<String, Object> userMap) throws CustomException {
		logger.info("Updating staff with criteria: {}", columnCriteria);
		Helper.checkNullValues(columnCriteria);
		columnCriteria.getFields().addAll(Arrays.asList("modified_at", "performed_by"));
		columnCriteria.getValues()
				.addAll(Arrays.asList(System.currentTimeMillis(), Helper.getThreadLocalValue().get("id")));

		Criteria criteria = new Criteria();
		criteria.setClazz(Staff.class);
		applyUserFilters(criteria, userMap);
		SQLHelper.update(columnCriteria, criteria);
		logger.info("Staff updated successfully.");
	}

	public Map<String, Object> getStaff(Long id) throws CustomException {
		if (id == -1) {
			id = (Long) Helper.getThreadLocalValue().get("id");
		}

		logger.info("Fetching staff with id: {}", id);
		Criteria staffJoinCriteria = Helper.buildJoinCriteria(Staff.class, Arrays.asList("user"),
				Arrays.asList("staff.user_id"), Arrays.asList("="), Arrays.asList("user.id"),
				Arrays.asList("staff.user_id"), Arrays.asList("="), Arrays.asList(id));
		staffJoinCriteria.setJoin(" JOIN ");
		List<Object> staffList = SQLHelper.get(staffJoinCriteria);
		logger.info("Fetched {} staff members successfully.", staffList.size());
		Map<String, Object> resultMap = new HashMap<String, Object>();
		resultMap.put("staff", staffList);
		return resultMap;
	}

	public void removeStaff() throws CustomException {
		logger.info("Removing staff.");
		ColumnCriteria columnCriteria = new ColumnCriteria();
		columnCriteria.setFields(Arrays.asList("status"));
		columnCriteria.setValues(Arrays.asList("Inactive"));

		Criteria staffCriteria = Helper.buildCriteria(Staff.class, Arrays.asList("user_id"), Arrays.asList("="),
				Arrays.asList(Helper.getThreadLocalValue().get("id")));
		SQLHelper.update(columnCriteria, staffCriteria);
		logger.info("Staff removed successfully.");
	}
}
