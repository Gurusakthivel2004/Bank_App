package dao;

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
		Object insertedValue = SQLHelper.insert(oauthProvider);

		return Helper.convertToLong(insertedValue);
	}
	
	public List<OauthProvider> get(Map<String, Object> oauthMap) throws Exception {
		Criteria criteria = DAOHelper.initializeCriteria(OauthProvider.class);
		DAOHelper.applyOauthProviderFilters(criteria, oauthMap);
		System.out.println(criteria);
		return SQLHelper.get(criteria, OauthProvider.class);
	}

	public void update(ColumnCriteria columnCriteria, Map<String, Object> oauthMap) throws Exception {
		logger.info("Updating oauth provider info{}", oauthMap);
		Criteria criteria = new Criteria().setClazz(OauthProvider.class);
		DAOHelper.applyOauthProviderFilters(criteria, oauthMap);

		SQLHelper.update(columnCriteria, criteria);
	}

	public long getDataCount(Map<String, Object> messageMap) throws Exception {
		return 0;
	}

}