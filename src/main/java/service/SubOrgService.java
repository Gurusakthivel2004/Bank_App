package service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import crm.LeadsService;
import dao.DAO;
import dao.DaoFactory;
import enums.Constants.HttpStatusCodes;
import enums.Constants.LogType;
import enums.Constants.Role;
import enums.Constants.TaskExecutor;
import model.ActivityLog;
import model.Org;
import model.OrgMember;
import model.SubOrg;
import model.SubOrgMember;
import util.CustomException;
import util.Helper;

public class SubOrgService {

	private static final Logger logger = LogManager.getLogger(SubOrgService.class);
	private static DAO<SubOrg> subOrgDAO = DaoFactory.getDAO(SubOrg.class);
	private static DAO<Org> orgDAO = DaoFactory.getDAO(Org.class);
	private static DAO<OrgMember> orgMemberDAO = DaoFactory.getDAO(OrgMember.class);
	private static DAO<SubOrgMember> subOrgMemberDAO = DaoFactory.getDAO(SubOrgMember.class);

	private SubOrgService() {}

	private static class SingletonHelper {
		private static final SubOrgService INSTANCE = new SubOrgService();
	}

	public static SubOrgService getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public void createOrg(Map<String, Object> orgMap) throws Exception {
		logger.info("Creating sub org, data: {}", orgMap);

		String name = Helper.validateAndGet(orgMap, "name", "Name of the sub org is missing.");
		String salaryBandString = Helper.validateAndGet(orgMap, "salaryBand", "Salary band of the sub org is missing.");
		String parentOrgName = Helper.validateAndGet(orgMap, "parentOrgName", "Name of the parent org is missing.");
		
		Long userId = (Long) Helper.getThreadLocalValue("id");
		validateOrgAccess(userId);

		Org parentOrg = getParentOrgByKey("name", parentOrgName);
		BigDecimal salaryBand = new BigDecimal(salaryBandString);

	
		SubOrg subOrg = saveSubOrg(name, parentOrg.getId(), salaryBand);
		logActivity(subOrg.getId());

		createMembers(parentOrg.getId(), subOrg.getId(), userId, Role.Admin);
		
		String email = parentOrgName + "@gmail.com";
		
		TaskExecutor.CRM.submitTask(() -> {
			try {
				LeadsService.getInstance().pushLead(subOrg, parentOrgName, email);
			} catch (Exception e) {
				logger.error("CRM Deals push failed: {}", e.getMessage(), e);
			}
		});
	
	}

	public void createMembers(Long orgId, Long subOrgId, Long userId, Role role) throws Exception {
		OrgMember orgMember = new OrgMember();
		orgMember.setOrgId(orgId);
		orgMember.setUserId(userId);
		orgMember.setUserTypeEnum(Role.Admin);

		try {

			logger.info("Inserting Org member data..");
			orgMemberDAO.create(orgMember);

		} catch (CustomException e) {
			if (e.getStatusCode() == HttpStatusCodes.CONFLICT.getCode()) {
				logger.warn("Org member already exists: " + e.getMessage());
			} else {
				throw e;
			}
		}

		if (subOrgId != null) {
			SubOrgMember subOrgMember = new SubOrgMember();
			subOrgMember.setSubOrgId(subOrgId);
			subOrgMember.setUserId(userId);
			subOrgMember.setUserTypeEnum(role);

			subOrgMemberDAO.create(subOrgMember);
		}
	}

	public Org getParentOrgByKey(String key, Object value) throws Exception {
		Map<String, Object> criteria = new HashMap<>();
		criteria.put(key, value);

		List<Org> orgList = orgDAO.get(criteria);
		if (orgList == null || orgList.isEmpty()) {
			throw new CustomException("Enter a valid criteria for fetching org.", HttpStatusCodes.BAD_REQUEST);
		}
		return orgList.get(0);
	}

	private SubOrg saveSubOrg(String name, Long parentOrgId, BigDecimal salaryBand) throws Exception {
		SubOrg subOrg = new SubOrg();
		subOrg.setName(name);
		subOrg.setOrgId(parentOrgId);
		subOrg.setSalaryBand(salaryBand);
		subOrg.setCreatedAt(System.currentTimeMillis());
		
		long subOrgId = subOrgDAO.create(subOrg);
		subOrg.setId(subOrgId);
		return subOrg;
	}

	public List<SubOrg> getSubOrgDetails(Map<String, Object> orgMap) throws Exception {
		logger.info("Fetching sub org info for account: {}", orgMap);
		if (orgMap.isEmpty()) {
			throw new CustomException("No Criteria provided.", HttpStatusCodes.BAD_REQUEST);
		}
		return subOrgDAO.get(orgMap);
	}
	
	private void validateOrgAccess(Long userId) throws Exception {
		Map<String, Object> criteriaMap = new HashMap<String, Object>();
		criteriaMap.put("userId", userId);
		
	    List<OrgMember> orgMembers = orgMemberDAO.get(criteriaMap);
	    if(orgMembers.isEmpty()) {
	    	throw new CustomException("User is not a member of this organization.", HttpStatusCodes.BAD_REQUEST);
	    }
	}

	private void logActivity(Long rowId) throws Exception {
		logger.debug("Logging org...");

		ActivityLog activityLog = new ActivityLog().setLogMessage("sub org created").setLogType(LogType.Insert)
				.setRowId(rowId).setTableName("subOrg").setPerformedBy((Long) Helper.getThreadLocalValue("id"))
				.setTimestamp(System.currentTimeMillis());

		Helper.logActivity(activityLog);
		logger.debug("Activity log created.");
	}

}