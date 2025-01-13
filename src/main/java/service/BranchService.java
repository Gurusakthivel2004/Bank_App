package service;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

import dao.BranchDAO;

import model.Branch;

import util.CustomException;
import util.Helper;

public class BranchService {

	private static final Logger logger = LogManager.getLogger(BranchService.class);
	private CacheService cacheService = new CacheService();
	private static BranchDAO branchDAO = new BranchDAO();

	public List<Object> getBranchDetails(Long branchId, boolean notExact) throws CustomException {
		logger.info("Fetching branch details for branchId: {}", branchId);

		try {
			String key = "branchInfo";
			Map<Long, List<Object>> cachedBranch = cacheService.get(key, new TypeReference<Map<Long, List<Object>>>() {
			});
			if (cachedBranch != null) {
				if (cachedBranch.containsKey(branchId)) {
					logger.info("Branch details for branchId: {} found in cache", branchId);
					return (List<Object>) cachedBranch.get(branchId);
				}
				logger.info("Adding branchId: {} to existing cachedBranch map", branchId);
			} else {
				logger.info("No existing cache found for key: {}. Initializing new map.", key);
				cachedBranch = new HashMap<>();
			}

			List<Object> branches = branchDAO.getBranch(branchId, notExact);
			if (branches.isEmpty()) {
				logger.warn("No branch details found for branchId: {}", branchId);
				return null;
			}
			cachedBranch.put(branchId, branches);
			cacheService.save(key, cachedBranch);
			logger.info("Updated cache with branchId: {} details", branchId);
			return branches;
		} catch (Exception e) {
			logger.error("Error fetching branch details: {}", e.getMessage(), e);
			throw new CustomException("Unable to fetch branch details. Please try again later.", e);
		}
	}

	public void createBranch(Map<String, Object> branchMap) throws CustomException {
		logger.info("Creating a new branch with data: {}", branchMap);

		try {
			branchMap.put("createdBy", Helper.getThreadLocalValue().get("id"));
			branchMap.put("status", "Active");

			logger.debug("Populated branchMap with additional data: {}", branchMap);

			Branch branch = Helper.createPojoFromMap(branchMap, Branch.class);
			logger.debug("Converted branchMap to Branch object: {}", branch);

			branchDAO.createBranch(branch);
			logger.info("Branch successfully created with name: {}", branch.getName());
		} catch (CustomException e) {
			logger.error("Error creating branch: {}", e.getMessage());
			throw e;
		} catch (Exception e) {
			logger.error("Unexpected error during branch creation: {}", e.getMessage());
			throw new CustomException("Branch creation failed. Please contact support.", e);
		}
	}

}
