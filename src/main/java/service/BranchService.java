package service;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

import cache.CacheUtil;
import dao.DAO;
import dao.DaoFactory;
import enums.Constants.HttpStatusCodes;
import enums.Constants.LogType;
import enums.Constants.Role;
import model.ActivityLog;
import model.Branch;
import util.CustomException;
import util.Helper;
import util.ValidationUtil;

public class BranchService {

	private static Logger logger = LogManager.getLogger(BranchService.class);
	private DAO<Branch> branchDAO = DaoFactory.getDAO(Branch.class);

	private BranchService() {}

	private static class SingletonHelper {
		private static final BranchService INSTANCE = new BranchService();
	}

	public static BranchService getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public List<Branch> getBranchDetails(Map<String, Object> branchMap) throws Exception {

		long branchId = Helper.parseLong(branchMap.getOrDefault("branchId", -1));
		boolean notExact = branchMap.containsKey("notExact");
		Role role = Role.fromString((String) Helper.getThreadLocalValue("role"));

		if (branchId <= 0) {
			throw new CustomException("Invalid branch id ", HttpStatusCodes.BAD_REQUEST);
		}
		String key = "branchInfo" + branchId;
		List<Branch> cachedBranch = CacheUtil.getCachedList(key, new TypeReference<List<Branch>>() {});
		if (cachedBranch != null && role == Role.Customer) {
			return cachedBranch;
		}
		
		logger.info("Fetching branch details for branchId: {}", branchId);
		List<Branch> branches = branchDAO.get(branchMap);
		if (branches.isEmpty()) {
			logger.warn("No branch details found for branchId: {}", branchId);
			throw new CustomException("No branch details found for branchId: " + branchId, HttpStatusCodes.BAD_REQUEST);
		}
		if (!notExact) {
			CacheUtil.save(key, branches);
		}
		return branches;
	}

	public void createBranch(Map<String, Object> branchMap) throws Exception {
		logger.info("Creating a new branch with data: {}", branchMap);

		Branch branch = Helper.createPojoFromMap(branchMap, Branch.class);
		ValidationUtil.validateBranchModel(branch);
		long branchId = branchDAO.create(branch);
		logger.info("Branch successfully created with name: {}", branch.getName());

		ActivityLog activityLog = new ActivityLog().setLogMessage("Branch created").setLogType(LogType.Insert)
				.setUserAccountNumber(null).setRowId(branchId).setTableName("Branch").setUserId(null);

		Helper.logActivity(activityLog);
	}

}
