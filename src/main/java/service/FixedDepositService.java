package service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dao.DAO;
import dao.DaoFactory;
import enums.Constants.AccountType;
import enums.Constants.HttpStatusCodes;
import enums.Constants.LogType;
import model.Account;
import model.ActivityLog;
import model.FixedDeposit;
import util.CustomException;
import util.Helper;

public class FixedDepositService {

	private static final Logger logger = LogManager.getLogger(FixedDepositService.class);
	private static DAO<Account> accountDAO = DaoFactory.getDAO(Account.class);
	private static DAO<FixedDeposit> fixedDepositDAO = DaoFactory.getDAO(FixedDeposit.class);
	private static final BigDecimal ANNUAL_RATE = new BigDecimal("6.5"); // 6.5%

	private FixedDepositService() {
	}

	private static class SingletonHelper {
		private static final FixedDepositService INSTANCE = new FixedDepositService();
	}

	public static FixedDepositService getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public void createFixedDeposit(Map<String, Object> fixedDepositMap, HttpSession session) throws Exception {
		logger.info("Starting FD creation process.");

		Long accountNumber = Long.parseLong((String) fixedDepositMap.get("accountNumber"));
		BigDecimal amount = new BigDecimal(fixedDepositMap.get("amount").toString());

		Integer duration = Integer.parseInt((String) fixedDepositMap.get("duration"));
		Long userId = (Long) Helper.getThreadLocalValue("id");

		Account account = validateAndGetAccount(accountNumber, userId, amount);

		long maturityEpochMillis = Helper.getFutureEpochMillisAfterMonths(duration);
		BigDecimal interest = calculateInterest(duration, amount, ANNUAL_RATE);

		logger.info("Creating FD for Account: {}, Duration: {} months, Interest: {}", accountNumber, duration,
				interest);

		FixedDeposit fixedDeposit = new FixedDeposit().setAccountNumber(accountNumber).setAmount(amount)
				.setInterestRate(interest).setStartDate(System.currentTimeMillis()).setMaturityDate(maturityEpochMillis)
				.setActive(true).setClosed(false);

		Long fdId = fixedDepositDAO.create(fixedDeposit);

		logger.info("FD created successfully. FD ID: {}", fdId);

		logActivity(accountNumber, userId, fdId);

		Map<String, Object> accountMap = new HashMap<>();
		accountMap.put("fetch", true);
		accountMap.put("branchId", account.getBranchId());

		List<Account> accounts = accountDAO.get(accountMap);

		Account operationalAccount = accounts.stream()
				.filter(acc -> acc.getAccountTypeEnum().equals(AccountType.Operational)).findFirst()
				.orElseThrow(() -> new CustomException("Invalid account number.", HttpStatusCodes.BAD_REQUEST));

		createTransaction(accountNumber, operationalAccount.getAccountNumber(), amount, account.getBranchId(), session);
	}

	private Account validateAndGetAccount(Long accountNumber, Long userId, BigDecimal amount) throws Exception {
		logger.debug("Validating account: {} for userId: {}", accountNumber, userId);

		Map<String, Object> accountMap = new HashMap<>();
		accountMap.put("userId", userId);

		List<Account> accounts = accountDAO.get(accountMap);
		logger.info(accounts);

		Account account = accounts.stream().filter(acc -> acc.getAccountNumber().equals(accountNumber)).findFirst()
				.orElseThrow(() -> new CustomException("Invalid account number.", HttpStatusCodes.BAD_REQUEST));

		if (amount.compareTo(new BigDecimal("10000")) < 0) {
			throw new CustomException("Minimum FD amount must be ₹10,000 or more.", HttpStatusCodes.BAD_REQUEST);
		}

		if (account.getBalance().compareTo(amount) < 0) {
			throw new CustomException("Insufficient Balance.", HttpStatusCodes.BAD_REQUEST);
		}

		logger.debug("Account validation successful.");
		return account;
	}

	// Interest= (P×R×T) \ 100​
	private static BigDecimal calculateInterest(Integer months, BigDecimal amount, BigDecimal annualRate) {
		BigDecimal monthsBD = new BigDecimal(months);
		BigDecimal twelve = new BigDecimal("12");

		BigDecimal time = monthsBD.divide(twelve, 4, RoundingMode.HALF_UP);
		BigDecimal interest = amount.multiply(annualRate).multiply(time).divide(new BigDecimal("100"), 2,
				RoundingMode.HALF_UP);

		logger.debug("Calculated Interest: {}, for months: {}, amount: {}", interest, months, amount);
		return interest;
	}

	private void logActivity(Long accountNumber, Long userId, Long rowId) throws Exception {
		logger.debug("Logging FD creation activity for account: {}", accountNumber);

		ActivityLog activityLog = new ActivityLog().setLogMessage("Fixed Deposit created").setLogType(LogType.Insert)
				.setRowId(rowId).setTableName("FixedDeposit").setUserId(userId).setUserAccountNumber(accountNumber)
				.setPerformedBy(userId).setTimestamp(System.currentTimeMillis());

		Helper.logActivity(activityLog);
		logger.debug("Activity log created.");
	}

	private void createTransaction(Long accountNumber, Long transactionAccountNumber, BigDecimal amount, Long branchId,
			HttpSession session) throws Exception {
		Map<String, Object> transactionMap = new HashMap<>();
		transactionMap.put("accountNumber", accountNumber.toString());
		transactionMap.put("transactionAccountNumber", transactionAccountNumber.toString());
		transactionMap.put("amount", amount.toString());
		transactionMap.put("branchId", branchId.toString());
		transactionMap.put("remarks", "Fixed Deposit");
		transactionMap.put("bankName", "Horizon");
		transactionMap.put("transactionIfsc", "");
		transactionMap.put("transactionType", "FixedDeposit");

		long txId = TransactionService.getInstance().prepareTransaction(transactionMap, null);

		session.setAttribute("txId", txId);
		TransactionService.getInstance().updateTransaction(txId);

	}
}
