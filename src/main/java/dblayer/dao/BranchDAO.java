package dblayer.dao;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import dblayer.model.Branch;
import dblayer.model.ColumnCriteria;
import dblayer.model.Criteria;
import util.CustomException;
import util.Helper;
import util.SQLHelper;

public class BranchDAO {

	private static final Logger logger = Logger.getLogger(BranchDAO.class.getName());

	public void createBranch(Branch branch) throws CustomException {
		logger.info("Starting branch creation...");

		Helper.checkNullValues(branch);
		branch.setCreatedAt(System.currentTimeMillis());
		branch.setPerformedBy((Long) Helper.getThreadLocalValue().get("id"));
		branch.setIfscCode("temp");

		logger.info("Inserting branch record into database...");
		Long branchId;
		try {
			branchId = ((BigInteger) SQLHelper.insert(branch)).longValue();
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error while inserting branch record", e);
			throw new CustomException("Failed to create branch", e);
		}

		logger.info("Branch created successfully with ID: " + branchId);

		String ifscCode = "HORI000" + String.format("%04d", branchId);
		ColumnCriteria columnCriteria = Helper.createColumnCriteria(Arrays.asList("ifscCode"), Arrays.asList(ifscCode));
		Criteria criteria = Helper.createCriteria(Branch.class, "id", "=", branchId);

		try {
			logger.info("Updating IFSC code for branch with ID: " + branchId);
			SQLHelper.update(columnCriteria, criteria);
			logger.info("IFSC code updated successfully: " + ifscCode);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error while updating IFSC code", e);
			throw new CustomException("Failed to update IFSC code for branch ID: " + branchId, e);
		}
	}

	public List<Object> getBranch(Long branchId, boolean notExact) throws CustomException {
		logger.info("Fetching branch details for ID: " + branchId);
		Criteria criteria = Helper.createCriteria(Branch.class, "id", "=", branchId);
		criteria.setSelectColumn(Arrays.asList("*"));
		try {
			if (notExact) {
				criteria.setColumn(Arrays.asList("id", "id"));
				criteria.setOperator(Arrays.asList("=", "LIKE"));
				criteria.setValue(Arrays.asList(branchId, "%" + branchId + "%"));
				criteria.setLimitValue(1);
				criteria.setLogicalOperator("OR");
			}
			List<Object> branches = SQLHelper.get(criteria);
			logger.info("Branch details fetched successfully for ID: " + branchId);
			return branches;
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error while fetching branch details for ID: " + branchId, e);
			throw new CustomException("Failed to fetch branch details for ID: " + branchId, e);
		}
	}

	public <T> void updateBranch(ColumnCriteria columnCriterias, Long branchId) throws CustomException {
		logger.info("Updating branch details for ID: " + branchId);
		Criteria criteria = Helper.createCriteria(Branch.class, "id", "=", branchId);
		try {
			SQLHelper.update(columnCriterias, criteria);
			logger.info("Branch details updated successfully for ID: " + branchId);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error while updating branch details for ID: " + branchId, e);
			throw new CustomException("Failed to update branch details for ID: " + branchId, e);
		}
	}
}
