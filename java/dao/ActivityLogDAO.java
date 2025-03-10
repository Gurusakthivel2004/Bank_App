package dao;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import model.ActivityLog;
import model.ColumnCriteria;
import model.Criteria;
import util.CustomException;
import util.Helper;
import util.SQLHelper;

public class ActivityLogDAO implements DAO<ActivityLog> {

	private static Logger logger = LogManager.getLogger(ActivityLogDAO.class);

	private ActivityLogDAO() {}

	private static class SingletonHelper {
		private static final ActivityLogDAO INSTANCE = new ActivityLogDAO();
	}

	public static ActivityLogDAO getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public long create(ActivityLog activityLog) throws Exception {
		logger.info("Inserting log details...");

		Helper.checkNullValues(activityLog);
		Long logId;

		logId = ((BigInteger) SQLHelper.insert(activityLog)).longValue();

		logger.info("Log created successfully with ID: " + logId);
		return logId;
	}

	public List<ActivityLog> get(Map<String, Object> logMap) throws Exception {
		Criteria criteria = DAOHelper.getLogCriteria(logMap);
		return SQLHelper.get(criteria, ActivityLog.class);

	}

	public long getDataCount(Map<String, Object> logMap) throws Exception {

		Criteria criteria = DAOHelper.getLogCriteria(logMap);
		criteria.setOffsetValue(-1L).setAggregateFunction("COUNT").setAggregateOperator("*");
		Long count = SQLHelper.getCount(criteria, ActivityLog.class);
		return count;

	}

	@Override
	public void update(ColumnCriteria columnCriteria, Map<String, Object> map) throws CustomException {
	}

}