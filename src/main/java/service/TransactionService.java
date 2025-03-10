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

import dao.AccountDAO;
import dao.DAO;
import dao.DaoFactory;
import enums.Constants.AccountType;
import enums.Constants.HttpStatusCodes;
import enums.Constants.Role;
import enums.Constants.TransactionStatus;
import enums.Constants.TransactionType;
import lock.LockWithCounter;
import model.Account;
import model.ColumnCriteria;
import model.Transaction;
import util.CustomException;
import util.Helper;
import util.ValidationUtil;

public class TransactionService {

	private static Logger logger = LogManager.getLogger(TransactionService.class.getName());
	private DAO<Transaction> transactionDAO = DaoFactory.getDAO(Transaction.class);
	private ConcurrentHashMap<Long, LockWithCounter> ACCOUNT_LOCKS = new ConcurrentHashMap<>();

	private TransactionService() {}

	private static class SingletonHelper {
		private static final TransactionService INSTANCE = new TransactionService();
	}

	public static TransactionService getInstance() {
		return SingletonHelper.INSTANCE;
	}

	private LockWithCounter getLock(long accountNumber) {
		return ACCOUNT_LOCKS.computeIfAbsent(accountNumber, k -> new LockWithCounter(new ReentrantLock(false)));
	}

	private void lockAccount(long accountNumber) {
		LockWithCounter lockWithCounter = getLock(accountNumber);
		lockWithCounter.lock();
	}

	private void releaseLock(long accountNumber) {
		LockWithCounter lockWithCounter = ACCOUNT_LOCKS.get(accountNumber);
		if (lockWithCounter != null && lockWithCounter.getLock().isHeldByCurrentThread()) {
			lockWithCounter.unlock();
			if (lockWithCounter.getCount().decrementAndGet() == 0) {
				ACCOUNT_LOCKS.remove(accountNumber);
			}
		}
	}

	public Map<String, Object> getTransactionDetails(Map<String, Object> txMap) throws Exception {
		long customerId = Long.parseLong(txMap.getOrDefault("customerId", 0L).toString());
		long accountNumber = Long.parseLong(txMap.getOrDefault("accountNumber", -1L).toString());

		if (customerId == -1L && accountNumber == -1) {
			customerId = (Long) Helper.getThreadLocalValue("id");
			txMap.put("customerId", customerId);
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
	}

	private Account fetchPrimaryAccount(Long customerId) throws Exception {
		Map<String, Object> accountMap = new HashMap<>();
		accountMap.put("userId", customerId);
		DAO<Account> accountDAO = AccountDAO.getInstance();
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
			throws Exception {
		long offset = (long) txMap.getOrDefault("offset", -1L);
		if (offset == 0) {
			long count = transactionDAO.getDataCount(txMap);
			txResult.put("count", count);
		}
	}

	public void prepareTransaction(Map<String, Object> transactionMap) throws Exception {
		long accountNumber = Long.parseLong((String) transactionMap.get("accountNumber"));
		long transactionAccountNumber = Long.parseLong((String) transactionMap.get("transactionAccountNumber"));

		TransactionType transactionType = TransactionType.fromString((String) transactionMap.get("transactionType"));
		logger.info("Initiating transaction creation...");

		long branchId = Helper.parseLong(transactionMap.getOrDefault("branchId", -1));
		Transaction transaction;
		Account account;
		BigDecimal amount = parseTransactionAmount(transactionMap), closingBalance;

		long firstLock = Math.min(accountNumber, transactionAccountNumber);
		long secondLock = Math.max(accountNumber, transactionAccountNumber);
		lockAccount(firstLock);
		lockAccount(secondLock);
		try {
			account = getAccount(accountNumber);
			ValidationUtil.validateTransactionAmount(amount, account);

			closingBalance = computeClosingBalance(transactionType, amount, account);
			updateAccountBalance(accountNumber, closingBalance);

			long customerId = account.getUserId();

			transactionMap.remove("branchId");
			transactionMap.put("customerId", customerId);
			transaction = Helper.createPojoFromMap(transactionMap, Transaction.class);

			transaction.setClosingBalance(closingBalance);

			prepareRecipientTransaction(transaction, "Horizon".equals(transaction.getBankName()), account, branchId);
			logger.info("Transaction successfully created for account number: {}", transaction.getAccountNumber());

		} finally {
			releaseLock(secondLock);
			releaseLock(firstLock);
		}
	}

	private Account getAccount(long accountNumber) throws Exception {
		Map<String, Object> accountMap = new HashMap<>();
		accountMap.put("accountNumber", accountNumber);
		DAO<Account> accountDao = AccountDAO.getInstance();
		List<Account> accounts = accountDao.get(accountMap);
		if (accounts == null || accounts.isEmpty()) {
			throw new CustomException("Account not found", HttpStatusCodes.BAD_REQUEST);
		}
		return accounts.get(0);
	}

	private void updateAccountBalance(long accountNumber, BigDecimal closingBalance) throws Exception {
		AccountDAO accountDAO = AccountDAO.getInstance();
		Map<String, Object> accountMap = new HashMap<>();
		accountMap.put("accountNumber", accountNumber);

		ColumnCriteria columnCriteria = new ColumnCriteria().setFields(Arrays.asList("balance"))
				.setValues(Arrays.asList(closingBalance));

		accountDAO.update(columnCriteria, accountMap);
	}

	private void checkRoleAuthorization(Role role, Transaction transaction, Account account, Long userId, long branchId)
			throws CustomException {
		if (branchId <= 0) {
			throw new CustomException("Invalid transaction inputs.", HttpStatusCodes.BAD_REQUEST);
		}
		if (role == Role.Employee) {
			if (!"Debit".equals(transaction.getTransactionType())
					&& account.getUserId().longValue() != userId.longValue()) {
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
			throws Exception {
		logger.info("Starting transaction processing...");
		Role role = Role.fromString((String) Helper.getThreadLocalValue("role"));
		checkRoleAuthorization(role, transaction, account, transaction.getCustomerId(), branchId);
		Long txId = createTransaction(transaction, account);

		if (thisBank) {
			processTransactionForThisBank(transaction, txId, account);
		}
	}

	private void processTransactionForThisBank(Transaction transaction, Long txId, Account account) throws Exception {

		Long transactionUserId = account.getUserId();
		updateTransactionDetails(transaction, transactionUserId, txId);

		try {
			long accountNumber = transaction.getAccountNumber();
			account = getAccount(accountNumber);
			ValidationUtil.validateTransactionAmount(transaction.getAmount(), account);

			BigDecimal closingBalance = computeClosingBalance(transaction.getTransactionTypeEnum(),
					transaction.getAmount(), account);
			updateAccountBalance(accountNumber, closingBalance);
			transaction.setCustomerId(account.getUserId());
			transaction.setClosingBalance(closingBalance);

			createTransaction(transaction, account);
		} catch (CustomException e) {
			cancelTransaction(txId);
			throw e;
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

	private void cancelTransaction(Long txId) throws Exception {
		ColumnCriteria columnCriteria = new ColumnCriteria().setFields(Arrays.asList("status"))
				.setValues(Arrays.asList(TransactionStatus.Cancelled));
		Map<String, Object> txMap = new HashMap<>();
		txMap.put("id", txId);
		txMap.put("status", "Failed");
		transactionDAO.update(columnCriteria, txMap);
	}

	public Long createTransaction(Transaction transaction, Account account) throws Exception {
		logger.info("Creating a new transaction...");
		setInitialTransactionDetails(transaction);

		ValidationUtil.validateModel(transaction, Transaction.class);
		long txId = saveTransaction(transaction);
		return txId;
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

	private long saveTransaction(Transaction transaction) throws Exception {
		long txId = transactionDAO.create(transaction);
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