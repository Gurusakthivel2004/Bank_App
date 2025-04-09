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
import enums.Constants.LoanAction;
import model.Loan;
import model.LoanLog;
import util.Helper;

public class LoanService {

	private DAO<Loan> loanDAO = DaoFactory.getDAO(Loan.class);
	private DAO<LoanLog> loanLogDAO = DaoFactory.getDAO(LoanLog.class);
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
		logLoanData(loan, loanId);
	}

	public List<Loan> getLoanDetails(Long accountNumber) throws Exception {
		logger.info("Fetching loan info for account: {}", accountNumber);
		Map<String, Object> loanCriteriaMap = new HashMap<>();
		loanCriteriaMap.put("accountNumber", accountNumber);

		return loanDAO.get(loanCriteriaMap);
	}

	private void logLoanData(Loan loan, Long loanId) throws Exception {
		Long userId = (Long) Helper.getThreadLocalValue("id");
		LoanLog loanLog = new LoanLog().setLoanId(loanId).setAccountNumber(loan.getAccountNumber())
				.setAction(LoanAction.Approve).setCreatedAt(System.currentTimeMillis()).setPerformedBy(userId)
				.setMessage("Loan approved.");
		BackgroundService.getInstance().submitTask("log", () -> {
			try {
				loanLogDAO.create(loanLog);
			} catch (Exception e) {
				logger.error("Error occurred while saving activity log", e);
			}
		});
	}

}