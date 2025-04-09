package dao;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import model.ColumnCriteria;
import model.Criteria;
import model.FixedDeposit;
import util.Helper;
import util.SQLHelper;

public class FixedDepositDAO implements DAO<FixedDeposit> {
	
	private static Logger logger = LogManager.getLogger(FixedDepositDAO.class);

	private FixedDepositDAO() {}

	private static class SingletonHelper {
		private static final FixedDepositDAO INSTANCE = new FixedDepositDAO();
	}

	public static FixedDepositDAO getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public long create(FixedDeposit fixedDeposit) throws Exception {
		logger.info("Inserting fixed deposit info...");

		Helper.checkNullValues(fixedDeposit);
		Object insertedValue = SQLHelper.insert(fixedDeposit);

		return Helper.convertToLong(insertedValue);
	}

	public List<FixedDeposit> get(Map<String, Object> fixedDepositMap) throws Exception {
		Criteria criteria = DAOHelper.initializeCriteria(FixedDeposit.class);
		DAOHelper.applyLoanFilters(criteria, fixedDepositMap);
		return SQLHelper.get(criteria, FixedDeposit.class);
	}

	public void update(ColumnCriteria columnCriteria, Map<String, Object> fixedDepositMap) throws Exception {
		logger.info("Updating fixedDeposit info{}", fixedDepositMap);
		Criteria criteria = new Criteria().setClazz(FixedDeposit.class);
		DAOHelper.applyLoanFilters(criteria, fixedDepositMap);
		SQLHelper.update(columnCriteria, criteria);
	}

	public long getDataCount(Map<String, Object> loanMap) throws Exception {
		return 0;
	}
}
