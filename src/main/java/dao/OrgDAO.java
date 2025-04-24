package dao;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import model.ColumnCriteria;
import model.Criteria;
import model.Org;
import util.Helper;
import util.SQLHelper;

public class OrgDAO implements DAO<Org> {

	private static Logger logger = LogManager.getLogger(OrgDAO.class);

	private OrgDAO() {}

	private static class SingletonHelper {
		private static final OrgDAO INSTANCE = new OrgDAO();
	}

	public static OrgDAO getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public void update(ColumnCriteria columnCriteria, Map<String, Object> orgMap) throws Exception {
		logger.info("Updating org details {}", orgMap);
		Criteria criteria = new Criteria().setClazz(Org.class);
		DAOHelper.addConditionIfPresent(criteria, orgMap, "id", "id", "EQUAL_TO", 0l);
		DAOHelper.addConditionIfPresent(criteria, orgMap, "name", "name", "EQUAL_TO", 0l);

		SQLHelper.update(columnCriteria, criteria);
	}

	public List<Org> get(Map<String, Object> orgMap) throws Exception {
		
		logger.info("Fetching org data: ", orgMap);
		Criteria criteria = new Criteria().setClazz(Org.class);
		criteria.setSelectColumn(Arrays.asList("*"));
		
		if (orgMap.containsKey("notExact")) {
			String orgName = (String) orgMap.get("name");
			criteria.setColumn(Arrays.asList("name", "name"))
			.setOperator(Arrays.asList("EQUAL_TO", "LIKE"))
			.setValue(Arrays.asList(orgName, "%" + orgName + "%"))
			.setLimitValue(5)
			.setLogicalOperator("OR");
		} else {
			DAOHelper.addConditionIfPresent(criteria, orgMap, "id", "id", "EQUAL_TO", 0l);
			DAOHelper.addConditionIfPresent(criteria, orgMap, "name", "name", "EQUAL_TO", "");
		}
		logger.info(criteria);
		return SQLHelper.get(criteria, Org.class);
	}

	public long create(Org org) throws Exception {
		logger.info("Inserting org info...");

		Helper.checkNullValues(org);
		Object insertedValue = SQLHelper.insert(org);

		return Helper.convertToLong(insertedValue);
	}

	public long getDataCount(Map<String, Object> txMap) throws Exception {
		return 0;
	}
}