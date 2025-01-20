package dao;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import Enum.Constants.HttpStatusCodes;
import model.ActivityLog;
import model.ColumnCriteria;
import model.Criteria;
import util.CustomException;
import util.Helper;
import util.SQLHelper;

public class ActivityLogDAO implements DAO<ActivityLog> {

	private static final Logger logger = LogManager.getLogger(ActivityLogDAO.class);

	public Long create(ActivityLog activityLog) throws CustomException {
		logger.info("Inserting log details...");

		Helper.checkNullValues(activityLog);
		Long userId = (Long) Helper.getThreadLocalValue("id");
		
		activityLog.setTimestamp(System.currentTimeMillis()).setPerformedBy(userId);

		Long logId;
		try {
			logId = ((BigInteger) SQLHelper.insert(activityLog)).longValue();
		} catch (CustomException ce) {
			throw ce;
		} catch (Exception e) {
			logger.error("Error while inserting log record", e);
			throw new CustomException("Failed to create log", HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}

		logger.info("Log created successfully with ID: " + logId);
		return logId;
	}

	public List<ActivityLog> get(Map<String, Object> logMap) throws CustomException {
		Criteria criteria = DAOHelper.getLogCriteria(logMap);
		try {
			return SQLHelper.get(criteria, ActivityLog.class);
		} catch (CustomException e) {
			throw e;
		} catch (SQLException e) {
			logger.error("Error while fetching log details: ", e);
			throw new CustomException("Failed to fetch log details: ", HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

	public Long getDataCount(Map<String, Object> logMap) throws CustomException {
		try {
			Criteria criteria = DAOHelper.getLogCriteria(logMap);
			criteria.setOffsetValue(-1L).setAggregateFunction("COUNT").setAggregateOperator("*");
			Long count = SQLHelper.getCount(criteria, ActivityLog.class);
			if (count == 0) {
				throw new CustomException("Unexpected error occured while fetching log details",
						HttpStatusCodes.INTERNAL_SERVER_ERROR);
			}
			return count;
		} catch (SQLException e) {
			logger.error("Error while fetching log details: ", e);
			throw new CustomException("Failed to fetch log details: ", HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public void update(ColumnCriteria columnCriteria, Map<String, Object> map) throws CustomException {
	}

}
