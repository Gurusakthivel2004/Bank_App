package dblayer.dao;

import java.util.Arrays;
import java.util.List;
import dblayer.model.*;
import util.CustomException;
import util.Helper;
import util.SQLHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UserDAO {
	private static final Logger logger = LogManager.getLogger(UserDAO.class);

	public List<User> getUser(String selectColumn, Object selectValue, boolean notExact) throws CustomException {
		logger.info("Fetching user with column: {}, value: {}", selectColumn, selectValue);
		Helper.checkNullValues(selectColumn);
		Criteria criteria = new Criteria();
		if (notExact) {
			criteria.setClazz(User.class);
			criteria.setSelectColumn(Arrays.asList(selectColumn));
			criteria.setColumn(Arrays.asList("id", "id"));
			criteria.setOperator(Arrays.asList("=", "LIKE"));
			criteria.setValue(Arrays.asList(selectValue, "%" + selectValue + "%"));
			criteria.setLimitValue(5);
			criteria.setLogicalOperator("OR");
		} else {
			criteria = Helper.buildCriteria(User.class, Arrays.asList(selectColumn), Arrays.asList("="),
					Arrays.asList(selectValue));
			criteria.setSelectColumn(Arrays.asList("*"));
		}
		List<User> users = SQLHelper.get(criteria);
		System.out.println(users);
		if (users == null) {
			logger.error("User does not exist for column: {}, value: {}", selectColumn, selectValue);
			throw new CustomException("User does not exist.");
		}

		logger.info("User fetched successfully.");
		return users;
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

	public void updateCustomer(ColumnCriteria columnCriteria) throws CustomException {
		logger.info("Updating customer with criteria: {}", columnCriteria);
		Helper.checkNullValues(columnCriteria);
		columnCriteria.getFields().add("modifiedAt");
		columnCriteria.getValues().add(System.currentTimeMillis());

		Criteria criteria = Helper.buildCriteria(CustomerDetail.class, Arrays.asList("user_id"), Arrays.asList("="),
				Arrays.asList(Helper.getThreadLocalValue().get("id")));
		SQLHelper.update(columnCriteria, criteria);
		logger.info("Customer updated successfully.");
	}

	public List<CustomerDetail> getCustomers(Long customerId) throws CustomException {
		logger.info("Fetching customers.");
		Criteria customerJoinCriteria = Helper.buildJoinCriteria(CustomerDetail.class,
				Arrays.asList("customer", "user"), Arrays.asList("customerDetail.user_id", "customer.user_id"),
				Arrays.asList("=", "="), Arrays.asList("customer.user_id", "user.id"),
				Arrays.asList("customerDetail.user_id"), Arrays.asList("="), Arrays.asList(customerId));

		List<CustomerDetail> customers = SQLHelper.get(customerJoinCriteria);
		logger.info("Fetched {} customers successfully.", customers.size());
		return customers;
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

	public void updateStaff(ColumnCriteria columnCriteria) throws CustomException {
		logger.info("Updating staff with criteria: {}", columnCriteria);
		Helper.checkNullValues(columnCriteria);
		columnCriteria.getFields().addAll(Arrays.asList("modified_at", "performed_by"));
		columnCriteria.getValues()
				.addAll(Arrays.asList(System.currentTimeMillis(), Helper.getThreadLocalValue().get("id")));

		Criteria criteria = Helper.buildCriteria(Staff.class, Arrays.asList("user_id"), Arrays.asList("="),
				Arrays.asList(Helper.getThreadLocalValue().get("id")));
		SQLHelper.update(columnCriteria, criteria);
		logger.info("Staff updated successfully.");
	}

	public List<Staff> getStaff(Long id) throws CustomException {
		if (id == -1) {
			id = (Long) Helper.getThreadLocalValue().get("id");
		}

		logger.info("Fetching staff with id: {}", id);
		Criteria staffJoinCriteria = Helper.buildJoinCriteria(Staff.class, Arrays.asList("user"),
				Arrays.asList("staff.user_id"), Arrays.asList("="), Arrays.asList("user.id"),
				Arrays.asList("staff.user_id"), Arrays.asList("="), Arrays.asList(id));

		List<Staff> staffList = SQLHelper.get(staffJoinCriteria);
		logger.info("Fetched {} staff members successfully.", staffList.size());
		return staffList;
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
