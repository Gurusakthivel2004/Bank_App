package dao;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import model.ColumnCriteria;
import model.Criteria;
import model.Loan;
import util.Helper;
import util.SQLHelper;

public class LoanDAO implements DAO<Loan> {
	
	private static Logger logger = LogManager.getLogger(LoanDAO.class);

	private LoanDAO() {}

	private static class SingletonHelper {
		private static final LoanDAO INSTANCE = new LoanDAO();
	}

	public static LoanDAO getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public long create(Loan otpVerifications) throws Exception {
		logger.info("Inserting loan info...");

		Helper.checkNullValues(otpVerifications);
		Object insertedValue = SQLHelper.insert(otpVerifications);

		return Helper.convertToLong(insertedValue);
	}

	public List<Loan> get(Map<String, Object> loanMap) throws Exception {
		Criteria criteria = DAOHelper.initializeCriteria(Loan.class);
		DAOHelper.applyLoanFilters(criteria, loanMap);
		return SQLHelper.get(criteria, Loan.class);
	}

	public void update(ColumnCriteria columnCriteria, Map<String, Object> otpMap) throws Exception {
		logger.info("Updating loan info{}", otpMap);
		Criteria criteria = new Criteria().setClazz(Loan.class);
		DAOHelper.applyLoanFilters(criteria, otpMap);
		SQLHelper.update(columnCriteria, criteria);
	}

	public long getDataCount(Map<String, Object> loanMap) throws Exception {
		return 0;
	}
}
