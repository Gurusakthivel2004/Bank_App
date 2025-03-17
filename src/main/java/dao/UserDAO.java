package dao;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import model.ColumnCriteria;
import model.Criteria;
import model.CustomerDetail;
import model.Staff;
import model.User;
import util.Helper;
import util.SQLHelper;

public class UserDAO<T extends User> implements DAO<T> {

	private static Logger logger = LogManager.getLogger(UserDAO.class);
	private static final Integer DEFAULT_VERSION = 1;

	private UserDAO() {
	}

	private static class SingletonHelper {
		private static final UserDAO<?> INSTANCE = new UserDAO<>();
	}

	public static UserDAO<?> getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public long create(T user) throws Exception {
		Helper.checkNullValues(user);
		if (user instanceof CustomerDetail) {
			((CustomerDetail) user).setCreatedAt(System.currentTimeMillis())
					.setPerformedBy((long) Helper.getThreadLocalValue("id"))
					.setPassword(Helper.hashPassword("default", DEFAULT_VERSION)).setModifiedAt(System.currentTimeMillis());
		} else if (user instanceof Staff) {
			((Staff) user).setCreatedAt(System.currentTimeMillis())
					.setPerformedBy((long) Helper.getThreadLocalValue("id"))
					.setPassword(Helper.hashPassword("default", DEFAULT_VERSION)).setModifiedAt(System.currentTimeMillis());
		}
		return ((BigInteger) SQLHelper.insert(user)).longValue();
	}

	@SuppressWarnings("unchecked")
	public void update(ColumnCriteria columnCriteria, Map<String, Object> userMap) throws Exception {
		Helper.checkNullValues(columnCriteria);
		columnCriteria.getFields().add("modifiedAt");
		columnCriteria.getValues().add(System.currentTimeMillis());
		Class<? extends User> clazz = (Class<? extends User>) userMap.get("userClass");
		Criteria criteria = new Criteria().setClazz(clazz);
		DAOHelper.applyUserFilters(criteria, userMap, clazz);

		SQLHelper.update(columnCriteria, criteria);
	}

	@SuppressWarnings("unchecked")
	public List<T> get(Map<String, Object> userMap) throws Exception {
		Class<T> clazz = (Class<T>) userMap.get("userClass");
		logger.info("Fetching {} details.", clazz.getSimpleName());
		Criteria criteria = DAOHelper.buildUserCriteria(userMap, clazz, userMap.containsKey("notExact"));
		return SQLHelper.get(criteria, clazz);
	}

	@SuppressWarnings("unchecked")
	public long getDataCount(Map<String, Object> userMap) throws Exception {
		Class<T> clazz = (Class<T>) userMap.get("userClass");
		Criteria criteria = DAOHelper.buildUserCriteria(userMap, clazz, userMap.containsKey("notExact"));
		criteria.setOffsetValue(-1L).setAggregateFunction("COUNT").setAggregateOperator("*");
		Long count = SQLHelper.getCount(criteria, clazz);
		return count;
	}

}