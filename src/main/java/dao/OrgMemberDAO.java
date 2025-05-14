package dao;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import model.ColumnCriteria;
import model.Criteria;
import model.OrgMember;
import util.Helper;
import util.SQLHelper;

public class OrgMemberDAO implements DAO<OrgMember> {

	private static Logger logger = LogManager.getLogger(OrgMemberDAO.class);

	private OrgMemberDAO() {}

	private static class SingletonHelper {
		private static final OrgMemberDAO INSTANCE = new OrgMemberDAO();
	}

	public static OrgMemberDAO getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public void update(ColumnCriteria columnCriteria, Map<String, Object> orgMap) throws Exception {
		logger.info("Updating user org map details {}", orgMap);
		Criteria criteria = new Criteria().setClazz(OrgMember.class);
		DAOHelper.addConditionIfPresent(criteria, orgMap, "userId", "user_id", "EQUAL_TO", 0l);

		SQLHelper.update(columnCriteria, criteria);
	}

	public List<OrgMember> get(Map<String, Object> orgMap) throws Exception {
		logger.info("Fetching user org map data");
		Criteria criteria = new Criteria().setClazz(OrgMember.class).setSelectColumn(Arrays.asList("*"));
		DAOHelper.addConditionIfPresent(criteria, orgMap, "userId", "user_id", "EQUAL_TO", 0l);
		DAOHelper.addConditionIfPresent(criteria, orgMap, "orgId", "org_id", "EQUAL_TO", 0l);
		DAOHelper.addConditionIfPresent(criteria, orgMap, "userType", "user_type", "EQUAL_TO", "");
		logger.info(criteria);
		return SQLHelper.get(criteria, OrgMember.class);
	}

	public long create(OrgMember org) throws Exception {
		logger.info("Inserting user org map info...");

		Helper.checkNullValues(org);
		SQLHelper.insert(org);
		return 0;
	}

	public long getDataCount(Map<String, Object> txMap) throws Exception {
		return 0;
	}
}