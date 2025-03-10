package service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dao.DAO;
import dao.DaoFactory;
import model.ActivityLog;

public class ActivityLogService {

	private static Logger logger = LogManager.getLogger(ActivityLogService.class);
	private static DAO<ActivityLog> logDao = DaoFactory.getDAO(ActivityLog.class);

	private ActivityLogService() {}

	private static class SingletonHelper {
		private static final ActivityLogService INSTANCE = new ActivityLogService();
	}

	public static ActivityLogService getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public Map<String, Object> getLogDetails(Map<String, Object> logMap) throws Exception {
		logger.info("Fetching log details..");
		Map<String, Object> logResult = new HashMap<>();

		List<ActivityLog> logs = logDao.get(logMap);
		long logCount = logDao.getDataCount(logMap);

		logResult.put("logs", logs);
		logResult.put("count", logCount);

		logger.info("Retrieved log details..");
		return logResult;
	}

}