package dao;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import model.ColumnCriteria;
import model.Criteria;
import model.LoanLog;
import util.Helper;
import util.SQLHelper;

public class LoanLogDAO implements DAO<LoanLog> {

	private static Logger logger = LogManager.getLogger(LoanLogDAO.class);

	private LoanLogDAO() {}

	private static class SingletonHelper {
		private static final LoanLogDAO INSTANCE = new LoanLogDAO();
	}

	public static LoanLogDAO getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public long create(LoanLog loanLog) throws Exception {
		logger.info("Inserting loan log info...");

		Helper.checkNullValues(loanLog);
		Object insertedValue = SQLHelper.insert(loanLog);

		return Helper.convertToLong(insertedValue);
	}

	public List<LoanLog> get(Map<String, Object> loanMap) throws Exception {
		Criteria criteria = DAOHelper.initializeCriteria(LoanLog.class);
		DAOHelper.applyLoanLogFilters(criteria, loanMap);
		return SQLHelper.get(criteria, LoanLog.class);
	}

	public void update(ColumnCriteria columnCriteria, Map<String, Object> otpMap) throws Exception {
		
	}

	public long getDataCount(Map<String, Object> loanMap) throws Exception {
		return 0;
	}
}
