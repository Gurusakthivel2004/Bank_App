package service;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dao.DAO;
import dao.DaoFactory;
import enums.Constants.LogType;
import model.ActivityLog;
import model.UserSession;
import util.Helper;

public class UserSessionService {

	private static Logger logger = LogManager.getLogger(UserSessionService.class);
	private DAO<UserSession> userSessionDAO = DaoFactory.getDAO(UserSession.class);

	private UserSessionService() {
	}

	private static class SingletonHelper {
		private static final UserSessionService INSTANCE = new UserSessionService();
	}

	public static UserSessionService getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public List<UserSession> getSessionDetails(Map<String, Object> sessionMap) throws Exception {
		logger.info("Fetching session details..");
		List<UserSession> userSessions = userSessionDAO.get(sessionMap);
		logger.info("Retrieved session details..");
		return userSessions;
	}

	public void createBranch(Map<String, Object> sessionMap) throws Exception {
		logger.info("Creating a new session with data: {}", sessionMap);

		UserSession userSession = Helper.createPojoFromMap(sessionMap, UserSession.class);

		long userSessionId = userSessionDAO.create(userSession);

		ActivityLog activityLog = new ActivityLog().setLogMessage("Session created").setLogType(LogType.Insert)
				.setUserAccountNumber(null).setRowId(userSessionId).setTableName("UserSession")
				.setUserId((Long) sessionMap.get("userId"));

		TaskExecutorService.getInstance().submit(activityLog);
	}

}
