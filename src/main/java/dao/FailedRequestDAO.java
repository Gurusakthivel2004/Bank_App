package dao;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import model.ColumnCriteria;
import model.Criteria;
import model.FailedRequest;
import util.Helper;
import util.SQLHelper;

public class FailedRequestDAO implements DAO<FailedRequest> {

	private static Logger logger = LogManager.getLogger(FailedRequestDAO.class);

	private FailedRequestDAO() {
	}

	private static class SingletonHelper {
		private static final FailedRequestDAO INSTANCE = new FailedRequestDAO();
	}

	public static FailedRequestDAO getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public void update(ColumnCriteria columnCriteria, Map<String, Object> orgMap) throws Exception {
		logger.info("Updating failed request details {}", orgMap);
		Criteria criteria = new Criteria().setClazz(FailedRequest.class);
		DAOHelper.addConditionIfPresent(criteria, orgMap, "id", "id", "EQUAL_TO", 0l);
		SQLHelper.update(columnCriteria, criteria);
	}

	public List<FailedRequest> get(Map<String, Object> orgMap) throws Exception {

		logger.info("Fetching failed request data");
		Criteria criteria = new Criteria().setClazz(FailedRequest.class).setSelectColumn(Arrays.asList("*"));
		DAOHelper.addConditionIfPresent(criteria, orgMap, "id", "id", "EQUAL_TO", 0l);
		return SQLHelper.get(criteria, FailedRequest.class);
	}

	public long create(FailedRequest org) throws Exception {
		logger.info("Inserting failed request info...");

		Helper.checkNullValues(org);
		Object insertedValue = SQLHelper.insert(org);

		return Helper.convertToLong(insertedValue);
	}

	public long getDataCount(Map<String, Object> txMap) throws Exception {
		return 0;
	}
}