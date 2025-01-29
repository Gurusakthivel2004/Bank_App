package service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import Enum.Constants.AccountType;
import Enum.Constants.HttpStatusCodes;
import Enum.Constants.LogType;
import Enum.Constants.Role;
import Enum.Constants.TransactionStatus;
import Enum.Constants.TransactionType;
import cache.CacheUtil;
import dao.AccountDAO;
import dao.BranchDAO;
import dao.DAO;
import dao.TransactionDAO;
import model.Account;
import model.ActivityLog;
import model.Branch;
import model.ColumnCriteria;
import model.Transaction;
import util.CustomException;
import util.Helper;
import util.ValidationUtil;

public class TransactionService {

	private final Logger logger = LogManager.getLogger(TransactionService.class.getName());
	private DAO<Transaction> transactionDAO = new TransactionDAO();

	public Map<String, Object> getTransactionDetails(Map<String, Object> txMap) throws CustomException {
		try {
			Long customerId = (Long) txMap.get("customerId");
			Long accountNumber = (Long) txMap.getOrDefault("accountNumber", 0L);
			System.out.println(txMap.keySet());
			System.out.println(txMap.values());
			if (customerId != null && customerId == -1L) {
				customerId = (Long) Helper.getThreadLocalValue("id");
				txMap.put("customerId", customerId);
			}
			if (accountNumber == 0L) {
				Account primaryAccount = fetchPrimaryAccount(customerId);
				accountNumber = primaryAccount != null ? primaryAccount.getAccountNumber() : accountNumber;
			}
			txMap.put("accountNumber", accountNumber);

			setRoleBasedAccess(txMap);
			Map<String, Object> txResult = new HashMap<>();
			addTransactionCountIfRequired(txMap, txResult);
			List<Transaction> transactions = transactionDAO.get(txMap);
			checkAuthorization(transactions);
			txResult.put("transactions", transactions);
			return txResult;

		} catch (Exception e) {
			logger.error("Error fetching transaction details: {}", e);
			throw new CustomException("Error fetching transaction details: " + e.getMessage(),
					HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

	private Account fetchPrimaryAccount(Long customerId) throws CustomException {
		Map<String, Object> accountMap = new HashMap<>();
		accountMap.put("userId", customerId);
		List<Account> accounts = new AccountDAO().get(accountMap);
		if (accounts == null || accounts.isEmpty()) {
			logger.error("No Accounts found for user {}", customerId);
			throw new CustomException("No accounts found for user " + customerId, HttpStatusCodes.NOT_FOUND);
		}
		return accounts.stream().filter(Account::getIsPrimary).findFirst()
				.orElseThrow(() -> new CustomException("No primary account found for user " + customerId,
						HttpStatusCodes.NOT_FOUND));
	}

	private void setRoleBasedAccess(Map<String, Object> txMap) throws CustomException {
		Role role = Role.fromString((String) Helper.getThreadLocalValue("role"));
		if (role == Role.Employee) {
			txMap.put("branchId", Helper.getThreadLocalValue("branchId"));
		}
	}

	private void addTransactionCountIfRequired(Map<String, Object> txMap, Map<String, Object> txResult)
			throws CustomException {
		Long offset = (Long) txMap.getOrDefault("offset", -1L);
		if (offset == 0) {
			Long count = transactionDAO.getDataCount(txMap);
			txResult.put("count", count);
		}
	}

	private void checkAuthorization(List<Transaction> transactions) throws CustomException {
		AuthorizationService authService = new AuthorizationService();
		if (!authService.isAuthorized("transaction", transactions)) {
			throw new CustomException("Not authorized to access transaction details", HttpStatusCodes.UNAUTHORIZED);
		}
	}

	public void prepareTransaction(Map<String, Object> transactionMap) throws CustomException {
		logger.info("Initiating transaction creation...");

		long branchId = Long.parseLong((String) transactionMap.get("branchId"));
		long accountNumber = Long.parseLong((String) transactionMap.get("accountNumber"));
		long transactionAccountNumber = Long.parseLong((String) transactionMap.get("transactionAccountNumber"));
		Long userId = (Long) Helper.getThreadLocalValue("id");
		Role role = Role.fromString((String) Helper.getThreadLocalValue("role"));

		Account account = getAccount(accountNumber);
		long customerId = account.getUserId();

		BigDecimal transactionAmount = parseTransactionAmount(transactionMap);
		ValidationUtil.validateTransactionAmount(transactionAmount, account);
		checkRoleAuthorization(role, transactionMap, account, userId, branchId);

		String ifsc = getIfsc(branchId);
		transactionMap.put("ifsc", ifsc);
		transactionMap.remove("branchId");
		logger.debug("IFSC code retrieved and set for branch ID: {}", branchId);

		if ("Horizon".equals(transactionMap.get("bankName"))) {
			handleHorizonTransaction(transactionMap, transactionAccountNumber);
		}

		transactionMap.put("customerId", customerId);
		Transaction transaction = Helper.createPojoFromMap(transactionMap, Transaction.class);

		logger.info("Transaction object validation passed. Proceeding with transaction creation...");
		// Make the transaction and invalidate cache
		prepareRecipientTransaction(transaction, "Horizon".equals(transaction.getBankName()));
		logger.info("Transaction successfully created for account number: {}", transaction.getAccountNumber());
	}

	private Account getAccount(long accountNumber) throws CustomException {
		Map<String, Object> accountMap = new HashMap<>();
		accountMap.put("accountNumber", accountNumber);
		List<Account> accounts = new AccountDAO().get(accountMap);
		if (accounts.isEmpty()) {
			throw new CustomException("Account not found", HttpStatusCodes.BAD_REQUEST);
		}
		return accounts.get(0);
	}

	private BigDecimal parseTransactionAmount(Map<String, Object> transactionMap) throws CustomException {
		try {
			double amount = Double.parseDouble((String) transactionMap.get("amount"));
			return BigDecimal.valueOf(amount);
		} catch (NumberFormatException e) {
			throw new CustomException("Invalid amount format", HttpStatusCodes.BAD_REQUEST);
		}
	}

	private void checkRoleAuthorization(Role role, Map<String, Object> transactionMap, Account account, Long userId,
			long branchId) throws CustomException {
		if (role == Role.Employee) {
			if (!"Debit".equals(transactionMap.get("type")) && account.getUserId() != userId) {
				throw new CustomException("Unauthorized account found", HttpStatusCodes.UNAUTHORIZED);
			}
			if (account.getBranchId() != branchId) {
				logger.warn("Branch ID mismatch for account number: {}", account.getAccountNumber());
				throw new CustomException("Invalid account", HttpStatusCodes.BAD_REQUEST);
			}
		}
		if (role == Role.Customer && account.getUserId() != userId) {
			throw new CustomException("Unauthorized account found", HttpStatusCodes.UNAUTHORIZED);
		}
	}

	private String getIfsc(long branchId) throws CustomException {
		DAO<Branch> branchDAO = new BranchDAO();
		Map<String, Object> branchMap = new HashMap<>();
		branchMap.put("notExact", false);
		branchMap.put("branchId", branchId);

		List<Branch> branches = branchDAO.get(branchMap);
		if (branches.isEmpty()) {
			throw new CustomException("Branch not found", HttpStatusCodes.BAD_REQUEST);
		}

		return branches.get(0).getIfscCode();
	}

	private void handleHorizonTransaction(Map<String, Object> transactionMap, long transactionAccountNumber)
			throws CustomException {
		logger.debug("Bank name is Horizon. Retrieving transaction IFSC...");

		Account transactionAccount = getAccount(transactionAccountNumber);
		if (transactionAccount == null) {
			throw new CustomException("Transaction Account does not exist", HttpStatusCodes.BAD_REQUEST);
		}

		DAO<Branch> branchDAO = new BranchDAO();
		Map<String, Object> branchMap = new HashMap<>();
		branchMap.put("branchId", transactionAccount.getBranchId());

		List<Branch> branches = branchDAO.get(branchMap);
		if (branches.isEmpty()) {
			throw new CustomException("Branch for transaction account not found", HttpStatusCodes.BAD_REQUEST);
		}

		String transactionIfsc = branches.get(0).getIfscCode();
		transactionMap.put("transactionIfsc", transactionIfsc);
	}

	public void prepareRecipientTransaction(Transaction transaction, boolean thisBank) throws CustomException {
		logger.info("Starting transaction processing...");
		Long txId = createTransaction(transaction);

		if (thisBank) {
			processTransactionForThisBank(transaction, txId);
		}
	}

	private void processTransactionForThisBank(Transaction transaction, Long txId) throws CustomException {
		Account account = getAccountForTransaction(transaction);
		TransactionType txType = transaction.getTransactionTypeEnum();

		Long transactionUserId = account.getUserId();
		updateTransactionDetails(transaction, account, transactionUserId, txId);

		if (txType == TransactionType.Debit) {
			transaction.setTransactionTypeEnum(TransactionType.Credit);
		} else if (txType == TransactionType.Default) {
			transaction.setTransactionTypeEnum(TransactionType.Withdraw);
		}

		try {
			createTransaction(transaction);
		} catch (Exception e) {
			cancelTransaction(txId);
		}
	}

	private void updateTransactionDetails(Transaction transaction, Account account, Long transactionUserId, Long txId) {
		String transactionIfsc = transaction.getTransactionIfsc();
		String ifsc = transaction.getIfsc();
		Long accountNumber = transaction.getAccountNumber();
		Long transactionAccountNumber = transaction.getTransactionAccountNumber();

		transaction.setIfsc(transactionIfsc);
		transaction.setTransactionIfsc(ifsc);
		transaction.setAccountNumber(transactionAccountNumber);
		transaction.setTransactionAccountNumber(accountNumber);
		transaction.setCustomerId(transactionUserId);
		transaction.setId(txId);
	}

	private void cancelTransaction(Long txId) throws CustomException {
		ColumnCriteria columnCriteria = new ColumnCriteria().setFields(Arrays.asList("status"))
				.setValues(Arrays.asList(TransactionStatus.Cancelled));
		Map<String, Object> txMap = new HashMap<>();
		txMap.put("id", txId);
		transactionDAO.update(columnCriteria, txMap);
	}

	public Long createTransaction(Transaction transaction) throws CustomException {
		try {
			logger.info("Creating a new transaction...");

			setInitialTransactionDetails(transaction);

			Account account = getAccountForTransaction(transaction);
			BigDecimal closingBalance = computeClosingBalance(transaction, account.getBalance());
			transaction.setClosingBalance(closingBalance);
			ValidationUtil.validateModel(transaction, Transaction.class);
			Long txId = saveTransaction(transaction);
			updateAccountBalance(transaction, closingBalance);
			clearAccountCache(transaction);
			logTransactionActivity(transaction, txId);

			return txId;
		} catch (Exception e) {
			logger.error("Unexpected error occurred while preparing transaction: {}", e.getMessage());
			throw new CustomException("Error occurred while processing. Please check your inputs.",
					HttpStatusCodes.BAD_REQUEST);
		}
	}

	private void setInitialTransactionDetails(Transaction transaction) {
		transaction.setTransactionStatusEnum(TransactionStatus.Completed);
		transaction.setTransactionTime(System.currentTimeMillis());
		transaction.setPerformedBy((Long) Helper.getThreadLocalValue("id"));
	}

	private Account getAccountForTransaction(Transaction transaction) throws CustomException {
		AccountDAO accountDAO = new AccountDAO();
		Map<String, Object> accountMap = new HashMap<>();
		accountMap.put("accountNumber", transaction.getAccountNumber());
		List<Account> accounts = accountDAO.get(accountMap);
		Helper.checkNullValues(accounts);

		return accounts.get(0);
	}

	private BigDecimal computeClosingBalance(Transaction transaction, BigDecimal accountBalance)
			throws CustomException {
		BigDecimal amount = transaction.getAmount();
		TransactionType transactionType = transaction.getTransactionTypeEnum();
		AccountDAO accountDAO = new AccountDAO();
		logger.debug("Processing transaction for account number: {} with type: {}", transaction.getAccountNumber(),
				transactionType);

		switch (transactionType) {
		case Credit:
			return accountBalance.add(amount);
		case Withdraw:
		case Debit:
			return accountBalance.subtract(amount);
		case Deposit:
			return computeDepositBalance(accountBalance, transaction, accountDAO);
		case Default:
			transaction.setTransactionTypeEnum(TransactionType.Withdraw);
			return accountBalance;
		default:
			logger.error("Invalid transaction type: {}", transactionType);
			throw new CustomException("Invalid transaction type: " + transactionType, HttpStatusCodes.BAD_REQUEST);
		}
	}

	private Long saveTransaction(Transaction transaction) throws CustomException {
		Long txId = transactionDAO.create(transaction);
		logger.info("Transaction created with ID: {}", txId);
		return txId;
	}

	private void updateAccountBalance(Transaction transaction, BigDecimal closingBalance) throws CustomException {
		AccountDAO accountDAO = new AccountDAO();
		Map<String, Object> accountMap = new HashMap<>();
		accountMap.put("accountNumber", transaction.getAccountNumber());

		ColumnCriteria columnCriteria = new ColumnCriteria().setFields(Arrays.asList("balance"))
				.setValues(Arrays.asList(closingBalance));

		accountDAO.update(columnCriteria, accountMap);
	}

	private void clearAccountCache(Transaction transaction) {
		CacheUtil cacheUtil = new CacheUtil();
		String accountKey = "accountInfo" + (transaction.getAccountNumber() % 10000);
		cacheUtil.delete(accountKey);
	}

	private void logTransactionActivity(Transaction transaction, Long txId) {
		ActivityLog activityLog = new ActivityLog().setLogMessage("Transaction created").setLogType(LogType.Insert)
				.setUserAccountNumber(transaction.getAccountNumber()).setRowId(txId).setTableName("Transaction")
				.setUserId(transaction.getCustomerId());

		TaskExecutorService.getInstance().submit(activityLog);
	}

	private BigDecimal computeDepositBalance(BigDecimal accountBalance, Transaction transaction, AccountDAO accountDAO)
			throws CustomException {
		logger.info("Computing deposit balance...");
		Map<String, Object> accountMap = new HashMap<>();
		accountMap.put("accountNumber", transaction.getAccountNumber());
		List<Account> accounts = accountDAO.get(accountMap);
		Account account = accounts.get(0);
		if (!(AccountType.Operational == account.getAccountTypeEnum())) {
			return accountBalance.add(transaction.getAmount());
		} else {
			return accountBalance.subtract(transaction.getAmount());
		}
	}

}