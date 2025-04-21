package service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dao.DAO;
import dao.DaoFactory;
import dao.OtpVerificationsDAO;
import enums.Constants.LogType;
import model.ActivityLog;
import model.Loan;
import util.Helper;

public class LoanService {

	private DAO<Loan> loanDAO = DaoFactory.getDAO(Loan.class);
	private static Logger logger = LogManager.getLogger(OtpVerificationsDAO.class);

	private LoanService() {}

	private static class SingletonHelper {
		private static final LoanService INSTANCE = new LoanService();
	}

	public static LoanService getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public void createLoan(Long accountNumber, BigDecimal amount) throws Exception {
		logger.info("Creating loan to: {} amount: {}", accountNumber, amount);
		Loan loan = new Loan();
		loan.setAccountNumber(accountNumber);
		loan.setAmount(amount);
		loan.setCreatedAt(System.currentTimeMillis());

		Long loanId = loanDAO.create(loan);
		logActivity(accountNumber, null, loanId);
	}

	public List<Loan> getLoanDetails(Long accountNumber) throws Exception {
		logger.info("Fetching loan info for account: {}", accountNumber);
		Map<String, Object> loanCriteriaMap = new HashMap<>();
		loanCriteriaMap.put("accountNumber", accountNumber);

		return loanDAO.get(loanCriteriaMap);
	}
	
	private void logActivity(Long accountNumber, Long userId, Long rowId) throws Exception {
		logger.debug("Logging loan activity for account: {}", accountNumber);

		ActivityLog activityLog = new ActivityLog().setLogMessage("Loan approved").setLogType(LogType.Insert)
				.setRowId(rowId).setTableName("Loan").setUserId(userId).setUserAccountNumber(accountNumber)
				.setPerformedBy(userId).setTimestamp(System.currentTimeMillis());

		Helper.logActivity(activityLog);
		logger.debug("Activity log created.");
	}

}