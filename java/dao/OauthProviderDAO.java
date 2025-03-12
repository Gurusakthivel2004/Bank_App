package dao;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import model.ColumnCriteria;
import model.Criteria;
import model.OauthProvider;
import util.Helper;
import util.SQLHelper;

public class OauthProviderDAO implements DAO<OauthProvider> {

	private static Logger logger = LogManager.getLogger(OauthProviderDAO.class);

	private OauthProviderDAO() {
	}

	private static class SingletonHelper {
		private static final OauthProviderDAO INSTANCE = new OauthProviderDAO();
	}

	public static OauthProviderDAO getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public long create(OauthProvider oauthProvider) throws Exception {
		logger.info("Inserting provider info...");

		Helper.checkNullValues(oauthProvider);
		Long oauthId;
		oauthId = ((BigInteger) SQLHelper.insert(oauthProvider)).longValue();

		logger.info("provider info created successfully with ID: " + oauthId);
		return oauthId;
	}

	public List<OauthProvider> get(Map<String, Object> oauthMap) throws Exception {
		Criteria criteria = DAOHelper.initializeCriteria(OauthProvider.class);
		DAOHelper.addConditionIfPresent(criteria, oauthMap, "provider", "provider", "EQUAL_TO", "");
		DAOHelper.addConditionIfPresent(criteria, oauthMap, "accessToken", "access_token", "EQUAL_TO", "");
		DAOHelper.addConditionIfPresent(criteria, oauthMap, "userId", "user_id", "EQUAL_TO", 0l);
		DAOHelper.addConditionIfPresent(criteria, oauthMap, "id", "id", "EQUAL_TO", 0l);
		return SQLHelper.get(criteria, OauthProvider.class);
	}

	public void update(ColumnCriteria columnCriteria, Map<String, Object> oauthMap) throws Exception {
		logger.info("Updating oauth provider info{}", oauthMap);
		Criteria criteria = new Criteria().setClazz(OauthProvider.class);
		DAOHelper.addConditionIfPresent(criteria, oauthMap, "provider", "provider", "EQUAL_TO", "");
		DAOHelper.addConditionIfPresent(criteria, oauthMap, "userId", "user_id", "EQUAL_TO", 0l);
		DAOHelper.addConditionIfPresent(criteria, oauthMap, "accessToken", "access_token", "EQUAL_TO", "");
		DAOHelper.addConditionIfPresent(criteria, oauthMap, "refreshToken", "refresh_token", "EQUAL_TO", "");

		SQLHelper.update(columnCriteria, criteria);
	}

	public long getDataCount(Map<String, Object> messageMap) throws Exception {
		return 0;
	}

}