package dao;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.Branch;
import model.ColumnCriteria;
import model.Criteria;
import util.CustomException;
import util.Helper;
import util.SQLHelper;

public class BranchDAO {

	private static final Logger logger = Logger.getLogger(BranchDAO.class.getName());

	public void createBranch(Branch branch) throws CustomException {
		logger.info("Starting branch creation...");

		Helper.checkNullValues(branch);
		branch.setCreatedAt(System.currentTimeMillis()).setPerformedBy((Long) Helper.getThreadLocalValue().get("id"))
				.setIfscCode("temp");

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
		ColumnCriteria columnCriteria = DAOHelper.createColumnCriteria(Arrays.asList("ifscCode"),
				Arrays.asList(ifscCode));
		Criteria criteria = DAOHelper.createCriteria(Branch.class, "id", "EQUAL_TO", branchId);

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
		Criteria criteria = DAOHelper.createCriteria(Branch.class, "id", "EQUAL_TO", branchId)
				.setSelectColumn(Arrays.asList("*"));
		try {
			if (notExact) {
				criteria.setColumn(Arrays.asList("id", "id")).setOperator(Arrays.asList("EQUAL_TO", "LIKE"))
						.setValue(Arrays.asList(branchId, "%" + branchId + "%")).setLimitValue(1)
						.setLogicalOperator("OR");
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
		Criteria criteria = DAOHelper.createCriteria(Branch.class, "id", "EQUAL_TO", branchId);
		try {
			SQLHelper.update(columnCriterias, criteria);
			logger.info("Branch details updated successfully for ID: " + branchId);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error while updating branch details for ID: " + branchId, e);
			throw new CustomException("Failed to update branch details for ID: " + branchId, e);
		}
	}
}
