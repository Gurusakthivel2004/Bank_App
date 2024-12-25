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

    private Criteria buildCriteria(Class<? extends MarkedClass> clazz, List<String> columns, List<String> operators, List<Object> values) {
        Criteria criteria = new Criteria();
        criteria.setClazz(clazz);
        criteria.setColumn(columns);
        criteria.setOperator(operators);
        criteria.setValue(values);
        return criteria;
    }

	private Criteria buildJoinCriteria(Class<? extends MarkedClass> clazz,
			List<Object> joinTable, List<String> joinColumn, List<String> joinOperator, 
                                        List<String> joinValue, List<String> columns, List<String> operators, List<Object> values) {
        Criteria criteria = buildCriteria(clazz, columns, operators, values);
        criteria.setJoinTable(joinTable);
        criteria.setJoinColumn(joinColumn);
        criteria.setJoinOperator(joinOperator);
        criteria.setJoinValue(joinValue);
        criteria.setSelectColumn(Arrays.asList("*"));
        return criteria;
    }

    public User getUser(String selectColumn, Object selectValue) throws CustomException {
        logger.info("Fetching user with column: {}, value: {}", selectColumn, selectValue);
        Helper.checkNullValues(selectColumn);

        Criteria userCriteria = buildCriteria(User.class, Arrays.asList(selectColumn), Arrays.asList("="), Arrays.asList(selectValue));
        userCriteria.setSelectColumn(Arrays.asList("*"));

        List<User> users = SQLHelper.get(userCriteria);
        System.out.println(users);
        if (users == null) {
            logger.error("User does not exist for column: {}, value: {}", selectColumn, selectValue);
            throw new CustomException("User does not exist.");
        }

        logger.info("User fetched successfully.");
        return users.get(0);
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

        Criteria criteria = buildCriteria(CustomerDetail.class, Arrays.asList("user_id"), Arrays.asList("="), 
                                          Arrays.asList(Helper.getThreadLocalValue().get("id")));
        SQLHelper.update(columnCriteria, criteria);
        logger.info("Customer updated successfully.");
    }

    public List<CustomerDetail> getCustomers(Long customerId) throws CustomException {
        logger.info("Fetching customers.");
        Criteria customerJoinCriteria = buildJoinCriteria(CustomerDetail.class, 
                Arrays.asList("customer", "user"), 
                Arrays.asList("customerDetail.user_id", "customer.user_id"), 
                Arrays.asList("=", "="), 
                Arrays.asList("customer.user_id", "user.id"), 
                Arrays.asList("customerDetail.user_id"), 
                Arrays.asList("="), 
                Arrays.asList(customerId));

        List<CustomerDetail> customers = SQLHelper.get(customerJoinCriteria);
        logger.info("Fetched {} customers successfully.", customers.size());
        return customers;
    }

    public void removeCustomer() throws CustomException {
        logger.info("Removing customer.");
        ColumnCriteria columnCriteria = new ColumnCriteria();
        columnCriteria.setFields(Arrays.asList("status"));
        columnCriteria.setValues(Arrays.asList("Inactive"));

        Criteria customerCriteria = buildCriteria(CustomerDetail.class, Arrays.asList("id"), Arrays.asList("="), 
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
        columnCriteria.getValues().addAll(Arrays.asList(System.currentTimeMillis(), Helper.getThreadLocalValue().get("id")));

        Criteria criteria = buildCriteria(Staff.class, Arrays.asList("user_id"), Arrays.asList("="), 
                                          Arrays.asList(Helper.getThreadLocalValue().get("id")));
        SQLHelper.update(columnCriteria, criteria);
        logger.info("Staff updated successfully.");
    }

    public List<Staff> getStaff(Long id) throws CustomException {
        if (id == -1) {
            id = (Long) Helper.getThreadLocalValue().get("id");
        }

        logger.info("Fetching staff with id: {}", id);
        Criteria staffJoinCriteria = buildJoinCriteria(Staff.class, 
                Arrays.asList("user"), 
                Arrays.asList("staff.user_id"), 
                Arrays.asList("="), 
                Arrays.asList("user.id"), 
                Arrays.asList("staff.user_id"), 
                Arrays.asList("="), 
                Arrays.asList(id));

        List<Staff> staffList = SQLHelper.get(staffJoinCriteria);
        logger.info("Fetched {} staff members successfully.", staffList.size());
        return staffList;
    }

    public void removeStaff() throws CustomException {
        logger.info("Removing staff.");
        ColumnCriteria columnCriteria = new ColumnCriteria();
        columnCriteria.setFields(Arrays.asList("status"));
        columnCriteria.setValues(Arrays.asList("Inactive"));

        Criteria staffCriteria = buildCriteria(Staff.class, Arrays.asList("user_id"), Arrays.asList("="), 
                                               Arrays.asList(Helper.getThreadLocalValue().get("id")));
        SQLHelper.update(columnCriteria, staffCriteria);
        logger.info("Staff removed successfully.");
    }
}
