package dblayer.dao;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import dblayer.model.Branch;
import dblayer.model.ColumnCriteria;
import dblayer.model.Criteria;
import dblayer.model.MarkedClass;
import util.CustomException;
import util.Helper;
import util.SQLHelper;

public class BranchDAO {

    private static final Logger logger = Logger.getLogger(BranchDAO.class.getName());

    // Utility method to create a basic Criteria object
    private Criteria createCriteria(Class<? extends MarkedClass> clazz, String column, String operator, Object value) {
        Criteria criteria = new Criteria();
        criteria.setClazz(clazz);
        if (column != null && operator != null && value != null) {
            criteria.getColumn().add(column);
            criteria.getOperator().add(operator);
            criteria.getValue().add(value);
        }
        return criteria;
    }

    // Utility method to set fields and values for ColumnCriteria
    private ColumnCriteria createColumnCriteria(List<String> fields, List<Object> values) {
        ColumnCriteria columnCriteria = new ColumnCriteria();
        columnCriteria.setFields(fields);
        columnCriteria.setValues(values);
        return columnCriteria;
    }

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
        ColumnCriteria columnCriteria = createColumnCriteria(
            Arrays.asList("ifscCode"),
            Arrays.asList(ifscCode)
        );

        Criteria criteria = createCriteria(Branch.class, "id", "=", branchId);

        try {
            logger.info("Updating IFSC code for branch with ID: " + branchId);
            SQLHelper.update(columnCriteria, criteria);
            logger.info("IFSC code updated successfully: " + ifscCode);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error while updating IFSC code", e);
            throw new CustomException("Failed to update IFSC code for branch ID: " + branchId, e);
        }
    }

    public List<Branch> getBranch(Long branchId) throws CustomException {
        logger.info("Fetching branch details for ID: " + branchId);

        Criteria criteria = createCriteria(Branch.class, "id", "=", branchId);
        criteria.setSelectColumn(Arrays.asList("*"));

        try {
            List<Branch> branches = SQLHelper.get(criteria);
            logger.info("Branch details fetched successfully for ID: " + branchId);
            return branches;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error while fetching branch details for ID: " + branchId, e);
            throw new CustomException("Failed to fetch branch details for ID: " + branchId, e);
        }
    }

    public <T> void updateBranch(ColumnCriteria columnCriterias, Long branchId) throws CustomException {
        logger.info("Updating branch details for ID: " + branchId);

        Criteria criteria = createCriteria(Branch.class, "id", "=", branchId);

        try {
            SQLHelper.update(columnCriterias, criteria);
            logger.info("Branch details updated successfully for ID: " + branchId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error while updating branch details for ID: " + branchId, e);
            throw new CustomException("Failed to update branch details for ID: " + branchId, e);
        }
    }
}
