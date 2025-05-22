package service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dao.DAO;
import dao.DaoFactory;
import enums.Constants.HttpStatusCodes;
import enums.Constants.LogType;
import enums.Constants.Role;
import enums.Constants.UseCase;
import model.ActivityLog;
import model.Org;
import model.OrgMember;
import model.User;
import util.CRMQueueManager;
import util.CustomException;
import util.Helper;

public class OrgService {

	private static final Logger logger = LogManager.getLogger(OrgService.class);
	private static DAO<Org> orgDAO = DaoFactory.getDAO(Org.class);
	private static DAO<OrgMember> orgMemberDAO = DaoFactory.getDAO(OrgMember.class);

	private OrgService() {}

	private static class SingletonHelper {
		private static final OrgService INSTANCE = new OrgService();
	}

	public static OrgService getInstance() {
		return SingletonHelper.INSTANCE;
	}

	private void validateUserNotInAnyOrg(Long userId) throws Exception {
		List<OrgMember> orgMembers = getOrgMemberDetails(userId);
		if (!orgMembers.isEmpty()) {
			throw new CustomException("User already exists in other org.", HttpStatusCodes.CONFLICT);
		}
	}

	private Org prepareOrgEntity(Map<String, Object> orgMap) throws CustomException {
		Org org = Helper.createPojoFromMap(orgMap, Org.class);
		org.setCreatedAt(System.currentTimeMillis());
		return org;
	}

	private void createOrgAdminMembership(Long userId, Long orgId) throws Exception {
		OrgMember orgMember = new OrgMember();
		orgMember.setUserId(userId);
		orgMember.setOrgId(orgId);
		orgMember.setUserTypeEnum(Role.Admin);
		orgMemberDAO.create(orgMember);
	}
	
	public Long getAdminId(Long orgId) throws Exception {
		DAO<OrgMember> orgMemberDao = DaoFactory.getDAO(OrgMember.class);

		Map<String, Object> orgMemberCriteria = new HashMap<>();
		orgMemberCriteria.put("orgId", orgId);
		orgMemberCriteria.put("userType", Role.Admin.name());
		
		List<OrgMember> orgMembers = orgMemberDao.get(orgMemberCriteria);

		if (orgMembers.isEmpty()) {
			throw new CustomException("Admin doesn't belong to an org.", HttpStatusCodes.BAD_REQUEST);
		}

		OrgMember orgMember = orgMembers.get(0);
		return orgMember.getUserId();
	}

	public Org getOrgByUserId(Long userId) throws Exception {
		DAO<OrgMember> orgMemberDao = DaoFactory.getDAO(OrgMember.class);

		Map<String, Object> orgMemberCriteria = new HashMap<>();
		orgMemberCriteria.put("userId", userId);

		List<OrgMember> orgMembers = orgMemberDao.get(orgMemberCriteria);

		if (orgMembers.isEmpty()) {
			throw new CustomException("User doesn't belong to an org.", HttpStatusCodes.BAD_REQUEST);
		}

		OrgMember orgMember = orgMembers.get(0);

		return getOrgById(orgMember.getOrgId());
	}
	
	public Org getOrgById(Long orgId) throws Exception {
		DAO<Org> orgDao = DaoFactory.getDAO(Org.class);
		Map<String, Object> orgCriteria = new HashMap<>();
		orgCriteria.put("id", orgId);

		List<Org> orgs = orgDao.get(orgCriteria);

		Org org = orgs.get(0);
		return org;
	}

	public void createOrg(Map<String, Object> orgMap) throws Exception {
		logger.info("Creating org, data: {}", orgMap);

		Long userId = (Long) Helper.getThreadLocalValue("id");
		validateUserNotInAnyOrg(userId);

		Org org = prepareOrgEntity(orgMap);
		Long orgId = orgDAO.create(org);

		logActivity(orgId);
		createOrgAdminMembership(userId, orgId);

		User user = UserService.getInstance().getUserById(userId);
		
		Map<String, Object> payload = new HashMap<>();
		payload.put("retries", 0);
		payload.put("orgId", org.getId().toString());
		payload.put("userId", user.getId().toString());
		payload.put("useCaseId", UseCase.ORG_PUSH.getId().toString());
		
		CRMQueueManager.addToInsertSet(payload);
	}

	public List<OrgMember> getOrgMemberDetails(Long userId) throws Exception {
		Map<String, Object> orgMemeberMap = new HashMap<String, Object>();
		orgMemeberMap.put("userId", userId);
		logger.info("Fetching org member info for org: {}", orgMemeberMap);

		return orgMemberDAO.get(orgMemeberMap);
	}

	public List<Org> getOrgDetails(Map<String, Object> orgMap) throws Exception {
		logger.info("Fetching org info for org: {}", orgMap);
		if (orgMap.isEmpty()) {
			throw new CustomException("No Criteria provided.", HttpStatusCodes.BAD_REQUEST);
		}
		return orgDAO.get(orgMap);
	}

	private void logActivity(Long rowId) throws Exception {
		logger.debug("Logging org...");

		ActivityLog activityLog = new ActivityLog().setLogMessage("Org created").setLogType(LogType.Insert)
				.setRowId(rowId).setTableName("Org").setPerformedBy((Long) Helper.getThreadLocalValue("id"))
				.setTimestamp(System.currentTimeMillis());

		Helper.logActivity(activityLog);
		logger.debug("Activity log created.");
	}

}