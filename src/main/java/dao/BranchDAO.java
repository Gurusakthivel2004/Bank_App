package dao;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import Enum.Constants.HttpStatusCodes;
import model.Branch;
import model.ColumnCriteria;
import model.Criteria;
import util.CustomException;
import util.Helper;
import util.SQLHelper;

public class BranchDAO {

	private static final Logger logger = LogManager.getLogger(BranchDAO.class);

	public void createBranch(Branch branch) throws CustomException {
		logger.info("Starting branch creation...");

		Helper.checkNullValues(branch);
		branch.setCreatedAt(System.currentTimeMillis()).setPerformedBy((Long) Helper.getThreadLocalValue().get("id"))
				.setIfscCode("temp");

		logger.info("Inserting branch record into database...");
		Long branchId;
		try {
			branchId = ((BigInteger) SQLHelper.insert(branch)).longValue();
		} catch (CustomException ce) {
			throw ce;
		} catch (Exception e) {
			logger.error("Error while inserting branch record", e);
			throw new CustomException("Failed to create branch", HttpStatusCodes.INTERNAL_SERVER_ERROR);
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
		} catch (SQLException e) {
			logger.error("Error while updating IFSC code", e);
			throw new CustomException("Failed to update IFSC code for branch ID: " + branchId,
					HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

	public List<Branch> getBranch(Long branchId, boolean notExact) throws CustomException {
		logger.info("Fetching branch details for ID: " + branchId);
		Criteria criteria = DAOHelper.createCriteria(Branch.class, "id", "EQUAL_TO", branchId)
				.setSelectColumn(Arrays.asList("*"));
		try {
			if (notExact) {
				criteria.setColumn(Arrays.asList("id", "id")).setOperator(Arrays.asList("EQUAL_TO", "LIKE"))
						.setValue(Arrays.asList(branchId, "%" + branchId + "%")).setLimitValue(1)
						.setLogicalOperator("OR");
			}
			List<Branch> branches = SQLHelper.get(criteria, Branch.class);
			if (branches.isEmpty()) {
				throw new CustomException("No result found for the id " + branchId, HttpStatusCodes.BAD_REQUEST);
			}
			logger.info("Branch details fetched successfully for ID: " + branchId);
			return branches;
		} catch (SQLException e) {
			logger.error("Error while fetching branch details for ID: " + branchId, e);
			throw new CustomException("Failed to fetch branch details for ID: " + branchId,
					HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

	public <T> void updateBranch(ColumnCriteria columnCriterias, Long branchId) throws CustomException {
		logger.info("Updating branch details for ID: " + branchId);
		Criteria criteria = DAOHelper.createCriteria(Branch.class, "id", "EQUAL_TO", branchId);
		try {
			SQLHelper.update(columnCriterias, criteria);
			logger.info("Branch details updated successfully for ID: " + branchId);
		} catch (Exception e) {
			logger.error("Error while updating branch details for ID: " + branchId, e);
			throw new CustomException("Failed to update branch details for ID: " + branchId,
					HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}
}
