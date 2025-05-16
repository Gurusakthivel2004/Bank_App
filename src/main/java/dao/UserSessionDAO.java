package dao;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import model.ColumnCriteria;
import model.Criteria;
import model.UserSession;
import util.Helper;
import util.SQLHelper;

public class UserSessionDAO implements DAO<UserSession> {

	private static Logger logger = LogManager.getLogger(UserSessionDAO.class);

	private UserSessionDAO() {
	}

	private static class SingletonHelper {
		private static final UserSessionDAO INSTANCE = new UserSessionDAO();
	}

	public static UserSessionDAO getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public long create(UserSession userSession) throws Exception {
		logger.info("Inserting session info...");
		Helper.checkNullValues(userSession);

		Object insertedValue = SQLHelper.insert(userSession);

		logger.info("Session info created successfully with ID: " + insertedValue);
		return Helper.convertToLong(insertedValue);
	}

	public List<UserSession> get(Map<String, Object> sessionMap) throws Exception {
		Criteria criteria = DAOHelper.initializeCriteria(UserSession.class);
		DAOHelper.addConditionIfPresent(criteria, sessionMap, "sessionId", "session_id", "EQUAL_TO", "");
		DAOHelper.addConditionIfPresent(criteria, sessionMap, "userId", "user_id", "EQUAL_TO", 0l);
		return SQLHelper.get(criteria, UserSession.class);
	}

	public void update(ColumnCriteria columnCriteria, Map<String, Object> sessionMap) throws Exception {
		logger.info("Removing session details {}", sessionMap);
		Criteria criteria = new Criteria().setClazz(UserSession.class);
		DAOHelper.addConditionIfPresent(criteria, sessionMap, "sessionId", "session_id", "EQUAL_TO", "");
		DAOHelper.addConditionIfPresent(criteria, sessionMap, "userId", "user_id", "EQUAL_TO", 0l);

		SQLHelper.delete(criteria);
	}

	public long getDataCount(Map<String, Object> messageMap) throws Exception {
		return 0;
	}

}