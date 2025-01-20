package service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

import Enum.Constants.HttpStatusCodes;
import cache.CacheUtil;
import dao.BranchDAO;
import dao.DAO;
import model.Branch;
import util.CustomException;
import util.Helper;

public class BranchService {

	private static final Logger logger = LogManager.getLogger(BranchService.class);

	private final CacheUtil cacheUtil = new CacheUtil();

	private static DAO<Branch> branchDAO = new BranchDAO();

	public List<Branch> getBranchDetails(Long branchId, boolean notExact) throws CustomException {
		logger.info("Fetching branch details for branchId: {}", branchId);
		String key = "branchInfo" + branchId;

		List<Branch> cachedBranch = cacheUtil.getCachedList(key, new TypeReference<List<Branch>>() {
		});
		AuthorizationService authService = new AuthorizationService();
		if (cachedBranch != null) {
			if (!authService.isAuthorized("branch", cachedBranch)) {
				throw new CustomException("Not authorized to access branch details", HttpStatusCodes.UNAUTHORIZED);
			}
			return cachedBranch;
		}

		Map<String, Object> branchMap = new HashMap<>();
		branchMap.put("notExact", notExact);
		branchMap.put("branchId", branchId);

		List<Branch> branches = branchDAO.get(branchMap);

		if (branches.isEmpty()) {
			logger.warn("No branch details found for branchId: {}", branchId);
			throw new CustomException("No branch details found for branchId: " + branchId, HttpStatusCodes.BAD_REQUEST);
		}
		if (!notExact) {
			cacheUtil.save(key, branches);
		}
		if (!authService.isAuthorized("branch", branches)) {
			throw new CustomException("Not authorized to access branch details", HttpStatusCodes.UNAUTHORIZED);
		}
		logger.info("Updated cache with branchId: {} details", branchId);
		return branches;
	}

	public void createBranch(Map<String, Object> branchMap) throws CustomException {
		logger.info("Creating a new branch with data: {}", branchMap);

		Branch branch = Helper.createPojoFromMap(branchMap, Branch.class);

		branchDAO.create(branch);
		logger.info("Branch successfully created with name: {}", branch.getName());
	}

}
