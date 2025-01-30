package service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import Enum.Constants.AccountType;
import Enum.Constants.HttpStatusCodes;
import Enum.Constants.LogType;
import Enum.Constants.Role;
import Enum.Constants.TransactionStatus;
import Enum.Constants.TransactionType;
import dao.AccountDAO;
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
	private static final ConcurrentHashMap<Long, Object> accountLocks = new ConcurrentHashMap<>();

	private TransactionService() {
	}

	private static class SingletonHelper {
		private static final TransactionService INSTANCE = new TransactionService();
	}

	public static TransactionService getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public Map<String, Object> getTransactionDetails(Map<String, Object> txMap) throws CustomException {
		try {
			Long customerId = (Long) txMap.get("customerId");
			Long accountNumber = (Long) txMap.getOrDefault("accountNumber", 0L);

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
		DAO<Account> accountDAO = new AccountDAO();
		List<Account> accounts = accountDAO.get(accountMap);
		if (accountMap == null || accountMap.isEmpty()) {
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
		} else if (role == Role.Customer) {
			txMap.put("customerId", Helper.getThreadLocalValue("id"));
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

	public void prepareTransaction(Map<String, Object> transactionMap) throws CustomException {
		long accountNumber = Long.parseLong((String) transactionMap.get("accountNumber"));
		long transactionAccountNumber = Long.parseLong((String) transactionMap.get("transactionAccountNumber"));

		Object firstAccountLock = accountLocks.computeIfAbsent(accountNumber, k -> new Object());
		Object secondAccountLock = accountLocks.computeIfAbsent(transactionAccountNumber, k -> new Object());

		synchronized (firstAccountLock) {
			synchronized (secondAccountLock) {
				logger.info("Initiating transaction creation...");

				long branchId = Long.parseLong((String) transactionMap.get("branchId"));

				Account account = getAccount(accountNumber);
				long customerId = account.getUserId();

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
				prepareRecipientTransaction(transaction, "Horizon".equals(transaction.getBankName()), account,
						branchId);
				logger.info("Transaction successfully created for account number: {}", transaction.getAccountNumber());
			}
		}
	}

	private Account getAccount(long accountNumber) throws CustomException {
		Map<String, Object> accountMap = new HashMap<>();
		accountMap.put("accountNumber", accountNumber);
		DAO<Account> accountDao = new AccountDAO();
		List<Account> accounts = accountDao.get(accountMap);
		if (accounts == null) {
			throw new CustomException("Account not found", HttpStatusCodes.BAD_REQUEST);
		}
		return accounts.get(0);
	}

	private void checkRoleAuthorization(Role role, Transaction transaction, Account account, Long userId, long branchId)
			throws CustomException {
		if (role == Role.Employee) {
			if (!"Debit".equals(transaction.getTransactionType()) && account.getUserId() != userId) {
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
		BranchService branchService = new BranchService();
		Map<String, Object> branchMap = new HashMap<>();
		branchMap.put("notExact", false);
		branchMap.put("branchId", branchId);

		List<Branch> branches = branchService.getBranchDetails(branchMap);
		return branches.get(0).getIfscCode();
	}

	private void handleHorizonTransaction(Map<String, Object> transactionMap, long transactionAccountNumber)
			throws CustomException {
		logger.debug("Bank name is Horizon. Retrieving transaction IFSC...");

		Account transactionAccount = getAccount(transactionAccountNumber);
		if (transactionAccount == null) {
			throw new CustomException("Transaction Account does not exist", HttpStatusCodes.BAD_REQUEST);
		}

		Map<String, Object> branchMap = new HashMap<>();
		branchMap.put("branchId", transactionAccount.getBranchId());

		List<Branch> branches = new BranchService().getBranchDetails(branchMap);
		if (branches.isEmpty()) {
			throw new CustomException("Branch for transaction account not found", HttpStatusCodes.BAD_REQUEST);
		}

		String transactionIfsc = branches.get(0).getIfscCode();
		transactionMap.put("transactionIfsc", transactionIfsc);
	}

	public void prepareRecipientTransaction(Transaction transaction, boolean thisBank, Account account, long branchId)
			throws CustomException {
		logger.info("Starting transaction processing...");
		Long userId = (Long) Helper.getThreadLocalValue("id");
		Role role = Role.fromString((String) Helper.getThreadLocalValue("role"));
		checkRoleAuthorization(role, transaction, account, userId, branchId);
		Long txId = createTransaction(transaction, account);

		if (thisBank) {
			processTransactionForThisBank(transaction, txId, account);
		}
	}

	private void processTransactionForThisBank(Transaction transaction, Long txId, Account account)
			throws CustomException {
		TransactionType txType = transaction.getTransactionTypeEnum();

		Long transactionUserId = account.getUserId();
		updateTransactionDetails(transaction, transactionUserId, txId);

		if (txType == TransactionType.Debit) {
			transaction.setTransactionTypeEnum(TransactionType.Credit);
		} else if (txType == TransactionType.Default) {
			transaction.setTransactionTypeEnum(TransactionType.Withdraw);
		}

		try {
			account = getAccount(transaction.getAccountNumber());
			createTransaction(transaction, account);
		} catch (Exception e) {
			cancelTransaction(txId);
		}
	}

	private void updateTransactionDetails(Transaction transaction, Long transactionUserId, Long txId) {
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

	public Long createTransaction(Transaction transaction, Account account) throws CustomException {
		try {
			logger.info("Creating a new transaction...");
			Long txId;
			setInitialTransactionDetails(transaction);

			try {
				BigDecimal amount = transaction.getAmount();

				if (amount.compareTo(BigDecimal.ZERO) <= 0) {
					throw new CustomException("Enter an amount greater than 0.", HttpStatusCodes.BAD_REQUEST);
				}

				BigDecimal accountBalance = account.getBalance();
				if (accountBalance.compareTo(amount) < 0) {
					throw new CustomException("Insufficient balance.", HttpStatusCodes.BAD_REQUEST);
				}

			} catch (NumberFormatException e) {
				throw new CustomException("Invalid amount format", HttpStatusCodes.BAD_REQUEST);
			}
			if (account.getAccountTypeEnum() == AccountType.Operational) {
				transaction.setTransactionTypeEnum(TransactionType.Default);
			}
			BigDecimal closingBalance = computeClosingBalance(transaction, account);
			transaction.setClosingBalance(closingBalance);
			ValidationUtil.validateModel(transaction, Transaction.class);
			txId = saveTransaction(transaction);
			updateAccountBalance(transaction, closingBalance);

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

	private BigDecimal computeClosingBalance(Transaction transaction, Account account) throws CustomException {
		BigDecimal accountBalance = account.getBalance();
		BigDecimal amount = transaction.getAmount();
		TransactionType transactionType = transaction.getTransactionTypeEnum();
		logger.debug("Processing transaction for account number: {} with type: {}", transaction.getAccountNumber(),
				transactionType);

		switch (transactionType) {
		case Credit:
			return accountBalance.add(amount);
		case Withdraw:
		case Debit:
			return accountBalance.subtract(amount);
		case Deposit:
			return computeDepositBalance(accountBalance, transaction, account);
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

	private void logTransactionActivity(Transaction transaction, Long txId) {
		ActivityLog activityLog = new ActivityLog().setLogMessage("Transaction created").setLogType(LogType.Insert)
				.setUserAccountNumber(transaction.getAccountNumber()).setRowId(txId).setTableName("Transaction")
				.setUserId(transaction.getCustomerId());

		TaskExecutorService.getInstance().submit(activityLog);
	}

	private BigDecimal computeDepositBalance(BigDecimal accountBalance, Transaction transaction, Account account)
			throws CustomException {
		logger.info("Computing deposit balance...");
		if (!(AccountType.Operational == account.getAccountTypeEnum())) {
			return accountBalance.add(transaction.getAmount());
		} else {
			return accountBalance.subtract(transaction.getAmount());
		}
	}

}