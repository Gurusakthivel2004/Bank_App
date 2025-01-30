package service;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

import Enum.Constants.HttpStatusCodes;
import Enum.Constants.LogType;
import Enum.Constants.Role;
import cache.CacheUtil;
import dao.BranchDAO;
import dao.DAO;
import model.ActivityLog;
import model.Branch;
import util.CustomException;
import util.Helper;
import util.ValidationUtil;

public class BranchService {

	private static final Logger logger = LogManager.getLogger(BranchService.class);

	private final CacheUtil cacheUtil = new CacheUtil();

	private static DAO<Branch> branchDAO = new BranchDAO();

	public List<Branch> getBranchDetails(Map<String, Object> branchMap) throws CustomException {

		Long branchId = (Long) branchMap.get("branchId");
		boolean notExact = branchMap.containsKey("notExact");
		Role role = Role.fromString((String) Helper.getThreadLocalValue("role"));

		if (branchId != null && branchId <= 0) {
			throw new CustomException("Invalid branch id ", HttpStatusCodes.BAD_REQUEST);
		}

		logger.info("Fetching branch details for branchId: {}", branchId);
		String key = "branchInfo" + branchId;

		List<Branch> cachedBranch = cacheUtil.getCachedList(key, new TypeReference<List<Branch>>() {
		});
		if (cachedBranch != null && role == Role.Customer) {
			return cachedBranch;
		}

		List<Branch> branches = branchDAO.get(branchMap);

		if (branches.isEmpty()) {
			logger.warn("No branch details found for branchId: {}", branchId);
			throw new CustomException("No branch details found for branchId: " + branchId, HttpStatusCodes.BAD_REQUEST);
		}
		if (!notExact) {
			cacheUtil.save(key, branches);
		}
		logger.info("Updated cache with branchId: {} details", branchId);
		return branches;
	}

	public void createBranch(Map<String, Object> branchMap) throws CustomException {
		logger.info("Creating a new branch with data: {}", branchMap);

		Branch branch = Helper.createPojoFromMap(branchMap, Branch.class);
		ValidationUtil.validateBranchModel(branch);
		Long branchId = branchDAO.create(branch);
		logger.info("Branch successfully created with name: {}", branch.getName());

		ActivityLog activityLog = new ActivityLog().setLogMessage("Branch created").setLogType(LogType.Insert)
				.setUserAccountNumber(null).setRowId(branchId).setTableName("Branch").setUserId(null);

		TaskExecutorService.getInstance().submit(activityLog);
	}

}
