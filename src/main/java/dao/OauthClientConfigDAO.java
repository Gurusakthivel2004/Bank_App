package dao;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import model.ColumnCriteria;
import model.Criteria;
import model.OauthClientConfig;
import model.OauthProvider;
import util.Helper;
import util.SQLHelper;

public class OauthClientConfigDAO implements DAO<OauthClientConfig> {

	private static Logger logger = LogManager.getLogger(OauthProviderDAO.class);

	private OauthClientConfigDAO() {}

	private static class SingletonHelper {
		private static final OauthClientConfigDAO INSTANCE = new OauthClientConfigDAO();
	}

	public static OauthClientConfigDAO getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public long create(OauthClientConfig oauthClientConfig) throws Exception {
		logger.info("Inserting client config info...");

		Helper.checkNullValues(oauthClientConfig);
		Object insertedValue = SQLHelper.insert(oauthClientConfig);

		return Helper.convertToLong(insertedValue);
	}

	public List<OauthClientConfig> get(Map<String, Object> oauthMap) throws Exception {
		Criteria criteria = DAOHelper.initializeCriteria(OauthProvider.class);
		DAOHelper.addConditionIfPresent(criteria, oauthMap, "provider", "provider", "EQUAL_TO", "");
		DAOHelper.addConditionIfPresent(criteria, oauthMap, "domain", "domain", "EQUAL_TO", "");
		DAOHelper.addConditionIfPresent(criteria, oauthMap, "id", "id", "EQUAL_TO", 0l);
		return SQLHelper.get(criteria, OauthClientConfig.class);
	}

	public void update(ColumnCriteria columnCriteria, Map<String, Object> oauthMap) throws Exception {
		logger.info("Updating oauth config info{}", oauthMap);
		Criteria criteria = new Criteria().setClazz(OauthClientConfig.class);
		DAOHelper.addConditionIfPresent(criteria, oauthMap, "provider", "provider", "EQUAL_TO", "");
		DAOHelper.addConditionIfPresent(criteria, oauthMap, "domain", "domain", "EQUAL_TO", "");
		DAOHelper.addConditionIfPresent(criteria, oauthMap, "id", "id", "EQUAL_TO", 0l);

		SQLHelper.update(columnCriteria, criteria);
	}

	public long getDataCount(Map<String, Object> messageMap) throws Exception {
		return 0;
	}

}