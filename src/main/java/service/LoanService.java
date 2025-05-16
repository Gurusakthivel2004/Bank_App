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
import enums.Constants.HttpStatusCodes;
import enums.Constants.Module;
import model.Account;
import model.Loan;
import model.ModuleLog;
import model.Org;
import model.User;
import util.CustomException;
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

	@SuppressWarnings("unchecked")
	private User fetchUser(Long accountNumber) throws Exception {
		Map<String, Object> accMap = new HashMap<>();
		accMap.put("accountNumber", accountNumber);

		List<Account> accounts = (List<Account>) AccountService.getInstance().getAccountDetails(accMap);
		if (accounts.isEmpty()) {
			throw new CustomException("Account does not exists.", HttpStatusCodes.BAD_REQUEST);
		}

		return UserService.getInstance().getUserById(accounts.get(0).getUserId());
	}

	public void createLoan(Long accountNumber, BigDecimal amount) throws Exception {
		logger.info("Creating loan to: {} amount: {}", accountNumber, amount);

		User user = fetchUser(accountNumber);
		Loan loan = new Loan();
		loan.setAccountNumber(accountNumber);
		loan.setAmount(amount);
		loan.setCreatedAt(System.currentTimeMillis());

		Long loanId = loanDAO.create(loan);
		logModule(accountNumber, user.getId(), loanId, amount.toString());
	}

	public List<Loan> getLoanDetails(Long accountNumber) throws Exception {
		logger.info("Fetching loan info for account: {}", accountNumber);
		Map<String, Object> loanCriteriaMap = new HashMap<>();
		loanCriteriaMap.put("accountNumber", accountNumber);

		return loanDAO.get(loanCriteriaMap);
	}

	private void logModule(Long accountNumber, Long userId, Long rowId, String amount) throws Exception {
		logger.debug("Logging FD creation activity for account: {}", accountNumber);

		ModuleLog moduleLog = new ModuleLog().setMessage("Loan created").setModule(Module.Loan).setModuleId(rowId)
				.setAccountNumber(accountNumber).setPerformedBy(userId).setCreatedAt(System.currentTimeMillis());

		Org org = OrgService.getInstance().getOrg(userId);
		Long adminId = OrgService.getInstance().getAdminId(org.getId());

		logger.debug("Module log created.");
		Helper.logAndPushModule(moduleLog, amount, adminId, org);
	}

}