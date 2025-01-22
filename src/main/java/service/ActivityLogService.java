package service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dao.ActivityLogDAO;
import dao.DAO;
import model.ActivityLog;
import util.CustomException;

public class ActivityLogService {

	private final Logger logger = LogManager.getLogger(ActivityLogService.class);

	public Map<String, Object> getLogDetails(Map<String, Object> logMap) throws CustomException {
		logger.info("Fetching log details..");
		Map<String, Object> logResult = new HashMap<>();

		DAO<ActivityLog> logDao = new ActivityLogDAO();
		List<ActivityLog> logs = logDao.get(logMap);
		Long logCount = logDao.getDataCount(logMap);

		logResult.put("logs", logs);
		logResult.put("count", logCount);

		logger.info("Retrieved log details..");
		return logResult;
	}

}
