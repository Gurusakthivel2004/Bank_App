package dao;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import model.ColumnCriteria;
import model.Criteria;
import model.SubOrg;
import util.Helper;
import util.SQLHelper;

public class SubOrgDAO implements DAO<SubOrg> {

	private static Logger logger = LogManager.getLogger(SubOrgDAO.class);

	private SubOrgDAO() {}

	private static class SingletonHelper {
		private static final SubOrgDAO INSTANCE = new SubOrgDAO();
	}

	public static SubOrgDAO getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public void update(ColumnCriteria columnCriteria, Map<String, Object> orgMap) throws Exception {
		logger.info("Updating org details {}", orgMap);
		Criteria criteria = new Criteria().setClazz(SubOrg.class);
		DAOHelper.addConditionIfPresent(criteria, orgMap, "id", "id", "EQUAL_TO", 0l);
		DAOHelper.addConditionIfPresent(criteria, orgMap, "name", "name", "EQUAL_TO", 0l);
		DAOHelper.addConditionIfPresent(criteria, orgMap, "orgId", "org_id", "EQUAL_TO", 0l);
		SQLHelper.update(columnCriteria, criteria);
	}

	public List<SubOrg> get(Map<String, Object> orgMap) throws Exception {
		
		logger.info("Fetching org data");
		Criteria criteria = new Criteria().setClazz(SubOrg.class);
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
			DAOHelper.addConditionIfPresent(criteria, orgMap, "orgId", "org_id", "EQUAL_TO", 0l);
		}
		
		return SQLHelper.get(criteria, SubOrg.class);
	}

	public long create(SubOrg org) throws Exception {
		logger.info("Inserting org info...");

		Helper.checkNullValues(org);
		Object insertedValue = SQLHelper.insert(org);

		return Helper.convertToLong(insertedValue);
	}

	public long getDataCount(Map<String, Object> txMap) throws Exception {
		return 0;
	}
}