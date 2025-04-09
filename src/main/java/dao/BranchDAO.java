package dao;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import enums.Constants.HttpStatusCodes;
import model.Branch;
import model.ColumnCriteria;
import model.Criteria;
import util.CustomException;
import util.Helper;
import util.SQLHelper;

public class BranchDAO implements DAO<Branch> {

	private static Logger logger = LogManager.getLogger(BranchDAO.class);
	
	private BranchDAO() {}

	private static class SingletonHelper {
		private static final BranchDAO INSTANCE = new BranchDAO();
	}

	public static BranchDAO getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public long create(Branch branch) throws Exception {
		logger.info("Starting branch creation...");

		Helper.checkNullValues(branch);
		branch.setCreatedAt(System.currentTimeMillis()).setPerformedBy((Long) Helper.getThreadLocalValue("id"))
				.setIfscCode("temp");

		Object insertedValue = SQLHelper.insert(branch);
		long branchId = Helper.convertToLong(insertedValue);

		logger.info("Branch created successfully with ID: " + branchId);

		String ifscCode = "HORI000" + String.format("%04d", branchId);
		ColumnCriteria columnCriteria = DAOHelper.createColumnCriteria(Arrays.asList("ifscCode"),
				Arrays.asList(ifscCode));
		Criteria criteria = DAOHelper.createCriteria(Branch.class, "id", "EQUAL_TO", branchId);

		SQLHelper.update(columnCriteria, criteria);
		logger.info("IFSC code updated successfully: " + ifscCode);

		return branchId;
	}

	public void update(ColumnCriteria columnCriterias, Map<String, Object> branchMap) throws Exception {
		Criteria criteria = new Criteria().setClazz(Branch.class);
		DAOHelper.addConditionIfPresent(criteria, branchMap, "branchId", "id", "EQUAL_TO", 0l);

		SQLHelper.update(columnCriterias, criteria);
		logger.info("Branch details updated successfully");

	}

	public List<Branch> get(Map<String, Object> branchMap) throws Exception {
		Long branchId = (Long) branchMap.get("branchId");
		Criteria criteria = DAOHelper.createCriteria(Branch.class, "id", "EQUAL_TO", branchId)
				.setSelectColumn(Arrays.asList("*"));

		if (branchMap.containsKey("notExact")) {
			criteria.setColumn(Arrays.asList("id", "id")).setOperator(Arrays.asList("EQUAL_TO", "LIKE"))
					.setValue(Arrays.asList(branchId, "%" + branchId + "%")).setLimitValue(1).setLogicalOperator("OR");
		}
		List<Branch> branches = SQLHelper.get(criteria, Branch.class);
		if (branches.isEmpty()) {
			throw new CustomException("No result found for the id " + branchId, HttpStatusCodes.BAD_REQUEST);
		}
		logger.info("Branch details fetched successfully for ID: " + branchId);
		return branches;

	}

	public long getDataCount(Map<String, Object> txMap) throws CustomException {
		return 0;
	}
}
