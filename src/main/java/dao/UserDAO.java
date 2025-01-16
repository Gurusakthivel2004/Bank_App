package dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import Enum.Constants.HttpStatusCodes;

import model.ColumnCriteria;
import model.Criteria;
import model.CustomerDetail;
import model.MarkedClass;
import model.Staff;
import model.User;

import util.CustomException;
import util.Helper;
import util.SQLHelper;

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
		Helper.checkNullValues(columnCriteria);
		columnCriteria.getFields().add("modifiedAt");
		columnCriteria.getValues().add(System.currentTimeMillis());

		Criteria criteria = new Criteria().setClazz(clazz);
		String idColumn = clazz == User.class ? "user.id" : "user_id";

		DAOHelper.applyUserFilters(criteria, userMap, idColumn);

		try {
			SQLHelper.update(columnCriteria, criteria);
		} catch (SQLException e) {
			logger.error("Error while updating {} details: ", clazz.getSimpleName(), e);
			throw new CustomException("Failed to update " + clazz.getSimpleName() + " details.",
					HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

	public <T extends User> List<T> getUserDetails(Map<String, Object> userMap, Class<T> clazz, boolean notExact)
			throws CustomException {
		logger.info("Fetching {} details.", clazz.getSimpleName());
		Criteria criteria = DAOHelper.buildUserCriteria(userMap, clazz, notExact);
		try {
			return SQLHelper.get(criteria, clazz);
		} catch (SQLException e) {
			logger.error("Error while fetching user details {}: ", clazz.getSimpleName(), e);
			throw new CustomException("Failed to fetch user details.", HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

	public <T extends MarkedClass> Long getDataCount(Map<String, Object> userMap, Class<T> clazz, boolean notExact)
			throws CustomException {
		try {
			Criteria criteria = DAOHelper.buildUserCriteria(userMap, clazz, notExact);
			criteria.setOffsetValue(-1L).setAggregateFunction("COUNT").setAggregateOperator("*");
			Long count = SQLHelper.getCount(criteria, clazz);
			return count;
		} catch (SQLException e) {
			logger.error("Error while fetching account details: ", e);
			throw new CustomException("Failed to fetch account details: ", HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

}
