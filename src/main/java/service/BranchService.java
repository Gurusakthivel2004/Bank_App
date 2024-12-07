package service;

import java.util.Map;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import dblayer.dao.BranchDAO;
import dblayer.model.Branch;
import util.CustomException;
import util.Helper;

/**
 * Service layer for managing branch-related operations.
 * Provides methods to fetch, create, and delete branch details.
 */
public class BranchService {

    private static final Logger logger = LogManager.getLogger(BranchService.class);

    private static BranchDAO branchDAO = new BranchDAO();

    /**
     * Retrieves a list of branches based on the provided parameters.
     * 
     * @param branchId   The unique identifier of the branch (0 if not used).
     * @return A list of matching branches.
     * @throws CustomException If an error occurs during branch retrieval.
     */
    public List<Branch> getBranchDetails(Long branchId) throws CustomException {
        logger.info("Fetching branch details for branchId: {}, branchName: {}", branchId);

        try {
            List<Branch> branches = branchDAO.getBranch(branchId);
            logger.debug("Retrieved {} branch(es) for the given criteria", branches.size());
            return branches;
        } catch (Exception e) {
            logger.error("Error fetching branch details: {}", e.getMessage());
            throw new CustomException("Unable to fetch branch details. Please try again later.", e);
        }
    }

    /**
     * Creates a new branch with the provided branch data.
     * 
     * @param branchMap A map containing the details of the branch to be created.
     * @throws CustomException If the branch creation process fails.
     */
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
