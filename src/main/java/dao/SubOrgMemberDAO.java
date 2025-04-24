package dao;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import model.ColumnCriteria;
import model.Criteria;
import model.OrgMember;
import model.SubOrgMember;
import util.Helper;
import util.SQLHelper;

public class SubOrgMemberDAO implements DAO<SubOrgMember> {

	private static Logger logger = LogManager.getLogger(SubOrgMemberDAO.class);

	private SubOrgMemberDAO() {}

	private static class SingletonHelper {
		private static final SubOrgMemberDAO INSTANCE = new SubOrgMemberDAO();
	}

	public static SubOrgMemberDAO getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public void update(ColumnCriteria columnCriteria, Map<String, Object> orgMap) throws Exception {
		logger.info("Updating user sub org map details {}", orgMap);
		Criteria criteria = new Criteria().setClazz(SubOrgMember.class);
		DAOHelper.addConditionIfPresent(criteria, orgMap, "userId", "user_id", "EQUAL_TO", 0l);

		SQLHelper.update(columnCriteria, criteria);
	}

	public List<SubOrgMember> get(Map<String, Object> orgMap) throws Exception {
		logger.info("Fetching user sub org map data");
		Criteria criteria = new Criteria().setClazz(OrgMember.class).setSelectColumn(Arrays.asList("*"));
		DAOHelper.addConditionIfPresent(criteria, orgMap, "userId", "user_id", "EQUAL_TO", 0l);
		return SQLHelper.get(criteria, SubOrgMember.class);
	}

	public long create(SubOrgMember org) throws Exception {
		logger.info("Inserting user sub org map info...");

		Helper.checkNullValues(org);
		SQLHelper.insert(org);
		return 0;
	}

	public long getDataCount(Map<String, Object> txMap) throws Exception {
		return 0;
	}
}