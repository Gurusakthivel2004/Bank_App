package service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import Enum.Constants.AccountType;
import Enum.Constants.HttpStatusCodes;
import Enum.Constants.Role;
import Enum.Constants.TransactionStatus;
import Enum.Constants.TransactionType;
import dao.AccountDAO;
import dao.DAO;
import dao.TransactionDAO;
import model.Account;
import model.ColumnCriteria;
import model.Transaction;
import util.CustomException;
import util.Helper;
import util.ValidationUtil;

public class TransactionService {

	private final Logger logger = LogManager.getLogger(TransactionService.class.getName());
	private DAO<Transaction> transactionDAO = new TransactionDAO();
	private static final Map<Long, ReentrantLock> LOCK_MAP = new ConcurrentHashMap<>();

	private TransactionService() {
	}

	private static class SingletonHelper {
		private static final TransactionService INSTANCE = new TransactionService();
	}

	public static TransactionService getInstance() {
		return SingletonHelper.INSTANCE;
	}

	private static ReentrantLock getLock(long accountNumber) {
		return LOCK_MAP.computeIfAbsent(accountNumber, k -> new ReentrantLock());
	}

	private void releaseLock(long accountNumber, ReentrantLock lock) {
		try {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
				LOCK_MAP.remove(accountNumber);
			} else {
				logger.warn("Lock is not held by the current thread for account: {}", accountNumber);
			}
		} catch (IllegalMonitorStateException e) {
			logger.warn("Failed to release lock for account {}: {}", accountNumber, e.getMessage());
		}
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

		TransactionType transactionType = TransactionType.fromString((String) transactionMap.get("transactionType"));
		logger.info("Initiating transaction creation...");

		long branchId = Long.parseLong((String) transactionMap.get("branchId"));
		Transaction transaction;
		Account account;
		BigDecimal amount = parseTransactionAmount(transactionMap), closingBalance;
		ReentrantLock lock = getLock(accountNumber);
		lock.lock();
		try {
			account = getAccount(accountNumber);
			ValidationUtil.validateTransactionAmount(amount, account);

			closingBalance = computeClosingBalance(transactionType, amount, account);
			updateAccountBalance(accountNumber, closingBalance);

		} finally {
			releaseLock(accountNumber, lock);
		}

		long customerId = account.getUserId();

		transactionMap.remove("branchId");

		transactionMap.put("customerId", customerId);
		transaction = Helper.createPojoFromMap(transactionMap, Transaction.class);

		transaction.setClosingBalance(closingBalance);

		prepareRecipientTransaction(transaction, "Horizon".equals(transaction.getBankName()), account, branchId);
		logger.info("Transaction successfully created for account number: {}", transaction.getAccountNumber());
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

	private void updateAccountBalance(long accountNumber, BigDecimal closingBalance) throws CustomException {
		AccountDAO accountDAO = new AccountDAO();
		Map<String, Object> accountMap = new HashMap<>();
		accountMap.put("accountNumber", accountNumber);

		ColumnCriteria columnCriteria = new ColumnCriteria().setFields(Arrays.asList("balance"))
				.setValues(Arrays.asList(closingBalance));

		accountDAO.update(columnCriteria, accountMap);
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

	private BigDecimal parseTransactionAmount(Map<String, Object> transactionMap) throws CustomException {
		try {
			double amount = Double.parseDouble((String) transactionMap.get("amount"));
			return BigDecimal.valueOf(amount);
		} catch (NumberFormatException e) {
			throw new CustomException("Invalid amount format", HttpStatusCodes.BAD_REQUEST);
		}
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

		Long transactionUserId = account.getUserId();
		updateTransactionDetails(transaction, transactionUserId, txId);
		long accountNumber = transaction.getAccountNumber();
		ReentrantLock lock = getLock(accountNumber);
		lock.lock();
		try {
			try {
				account = getAccount(accountNumber);
				ValidationUtil.validateTransactionAmount(transaction.getAmount(), account);

				BigDecimal closingBalance = computeClosingBalance(transaction.getTransactionTypeEnum(),
						transaction.getAmount(), account);
				updateAccountBalance(accountNumber, closingBalance);
				transaction.setClosingBalance(closingBalance);

			} finally {
				releaseLock(accountNumber, lock);
			}

			createTransaction(transaction, account);
		} catch (Exception e) {
			cancelTransaction(txId);
		}
	}

	private void updateTransactionDetails(Transaction transaction, Long transactionUserId, Long txId) {
		Long accountNumber = transaction.getAccountNumber();
		Long transactionAccountNumber = transaction.getTransactionAccountNumber();
		TransactionType txType = transaction.getTransactionTypeEnum();

		transaction.setAccountNumber(transactionAccountNumber);
		transaction.setTransactionAccountNumber(accountNumber);
		transaction.setCustomerId(transactionUserId);
		transaction.setId(txId);
		if (txType == TransactionType.Debit) {
			transaction.setTransactionTypeEnum(TransactionType.Credit);
		} else if (txType == TransactionType.Default) {
			transaction.setTransactionTypeEnum(TransactionType.Withdraw);
		}
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

			ValidationUtil.validateModel(transaction, Transaction.class);
			txId = saveTransaction(transaction);

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

	private BigDecimal computeClosingBalance(TransactionType transactionType, BigDecimal amount, Account account)
			throws CustomException {
		BigDecimal accountBalance = account.getBalance();
		logger.debug("Processing transaction for account number: {} with type: {}", account.getAccountNumber(),
				transactionType);
		switch (transactionType) {
		case Credit:
			return accountBalance.add(amount);
		case Withdraw:
		case Debit:
			if (account.getAccountTypeEnum() == AccountType.Operational) {
				return accountBalance;
			}
			return accountBalance.subtract(amount);
		case Deposit:
			return computeDepositBalance(accountBalance, amount, account);
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

	private BigDecimal computeDepositBalance(BigDecimal accountBalance, BigDecimal amount, Account account)
			throws CustomException {
		logger.info("Computing deposit balance...");
		if (!(AccountType.Operational == account.getAccountTypeEnum())) {
			return accountBalance.add(amount);
		} else {
			return accountBalance.subtract(amount);
		}
	}

}