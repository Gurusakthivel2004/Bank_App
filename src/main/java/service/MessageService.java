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

public class MessageService {

	private final Logger logger = LogManager.getLogger(MessageService.class);

	public Map<String, Object> getMessageDetails(Map<String, Object> msgMap) throws CustomException {
		logger.info("Fetching log details..");
		Map<String, Object> logResult = new HashMap<>();

		DAO<ActivityLog> logDao = new ActivityLogDAO();
		List<ActivityLog> logs = logDao.get(msgMap);
		Long logCount = logDao.getDataCount(msgMap);

		logResult.put("logs", logs);
		logResult.put("count", logCount);

		logger.info("Retrieved log details..");
		return logResult;
	}

}
