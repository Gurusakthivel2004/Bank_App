package service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

import cache.CacheUtil;
import dao.DAO;
import dao.DaoFactory;
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
		List<UserSession> userSessions = new ArrayList<UserSession>();
		String sessionId = null;
		
		if (sessionMap.containsKey("sessionId")) {
			sessionId = sessionMap.get("sessionId").toString();
			UserSession userSession = CacheUtil.get(sessionId, new TypeReference<UserSession>() {
			});
			if (userSession != null) {
				userSessions.add(userSession);
				return userSessions;
			}
		}

		userSessions = userSessionDAO.get(sessionMap);

		if (userSessions != null && !userSessions.isEmpty() && sessionId != null) {
			CacheUtil.save(sessionId, userSessions.get(0));
		}

		logger.info("Retrieved session details..");
		return userSessions;
	}

	public void createSession(Map<String, Object> sessionMap) throws Exception {
		logger.info("Creating a new session with data: {}", sessionMap);

		UserSession userSession = Helper.createPojoFromMap(sessionMap, UserSession.class);

		userSessionDAO.create(userSession);
	}

	public void deleteSession(Map<String, Object> sessionMap) throws Exception {
		logger.info("Removing session with data: {}", sessionMap);

		userSessionDAO.update(null, sessionMap);
	}

}
