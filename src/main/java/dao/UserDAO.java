package dao;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import Enum.Constants.HttpStatusCodes;
import model.ColumnCriteria;
import model.Criteria;
import model.CustomerDetail;
import model.Staff;
import model.User;
import util.CustomException;
import util.Helper;
import util.SQLHelper;

public class UserDAO<T extends User> implements DAO<T> {

	private static final Logger logger = LogManager.getLogger(UserDAO.class);

	public Long create(T user) throws CustomException {
		try {
			Helper.checkNullValues(user);
			if (user instanceof CustomerDetail) {
				((CustomerDetail) user).setCreatedAt(System.currentTimeMillis())
						.setPerformedBy((long) Helper.getThreadLocalValue("id"))
						.setPassword(Helper.hashPassword(((CustomerDetail) user).getPassword()));
			} else if (user instanceof Staff) {
				((Staff) user).setCreatedAt(System.currentTimeMillis())
						.setPerformedBy((long) Helper.getThreadLocalValue("id"))
						.setPassword(Helper.hashPassword(((Staff) user).getPassword()));
			}
			return ((BigInteger) SQLHelper.insert(user)).longValue();
		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException("An error occurred while creating. Please try later.",
					HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

	@SuppressWarnings("unchecked")
	public void update(ColumnCriteria columnCriteria, Map<String, Object> userMap) throws CustomException {
		Helper.checkNullValues(columnCriteria);
		columnCriteria.getFields().add("modifiedAt");
		columnCriteria.getValues().add(System.currentTimeMillis());
		Class<? extends User> clazz = (Class<? extends User>) userMap.get("userClass");
		Criteria criteria = new Criteria().setClazz(clazz);
		DAOHelper.applyUserFilters(criteria, userMap, clazz);

		try {
			SQLHelper.update(columnCriteria, criteria);
		} catch (SQLException e) {
			logger.error("Error while updating {} details: ", clazz.getSimpleName(), e);
			throw new CustomException("Failed to update " + clazz.getSimpleName() + " details.",
					HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

	@SuppressWarnings("unchecked")
	public List<T> get(Map<String, Object> userMap) throws CustomException {

		Class<T> clazz = (Class<T>) userMap.get("userClass");
		logger.info("Fetching {} details.", clazz.getSimpleName());
		Criteria criteria = DAOHelper.buildUserCriteria(userMap, clazz, userMap.containsKey("notExact"));
		try {
			return SQLHelper.get(criteria, clazz);
		} catch (SQLException e) {
			logger.error("Error while fetching user details {}: ", clazz.getSimpleName(), e);
			throw new CustomException("Failed to fetch user details.", HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

	@SuppressWarnings("unchecked")
	public Long getDataCount(Map<String, Object> userMap) throws CustomException {
		try {
			Class<T> clazz = (Class<T>) userMap.get("userClass");
			Criteria criteria = DAOHelper.buildUserCriteria(userMap, clazz, userMap.containsKey("notExact"));
			criteria.setOffsetValue(-1L).setAggregateFunction("COUNT").setAggregateOperator("*");
			Long count = SQLHelper.getCount(criteria, clazz);
			return count;
		} catch (SQLException e) {
			logger.error("Error while fetching account details: ", e);
			throw new CustomException("Failed to fetch account details: ", HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

}
