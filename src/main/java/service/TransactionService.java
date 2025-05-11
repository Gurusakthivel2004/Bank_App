package service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.http.HttpSession;

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

	private static Logger logger = LogManager.getLogger(TransactionService.class);
	private DAO<Transaction> transactionDAO = DaoFactory.getDAO(Transaction.class);
	private ConcurrentHashMap<Long, LockWithCounter> ACCOUNT_LOCKS = new ConcurrentHashMap<>();

	private TransactionService() {
	}

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

	public long prepareTransaction(Map<String, Object> transactionMap, HttpSession session) throws Exception {
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
			logger.info(account);
			logger.info(amount);
			ValidationUtil.validateTransactionAmount(amount, account);

			closingBalance = computeClosingBalance(transactionType, amount, account);
//			updateAccountBalance(accountNumber, closingBalance);

			long customerId = account.getUserId();

			transactionMap.remove("branchId");
			transactionMap.put("customerId", customerId);
			transaction = Helper.createPojoFromMap(transactionMap, Transaction.class);

			transaction.setClosingBalance(closingBalance);

			long txId = prepareRecipientTransaction(transaction, "Horizon".equals(transaction.getBankName()), account,
					branchId);
			logger.info("Transaction successfully created for account number: {}", transaction.getAccountNumber());
			if (!transaction.getTransactionTypeEnum().equals(TransactionType.FixedDeposit)) {
				NotificationService.getInstance().sendOtp(customerId, accountNumber, null);
			}
			if (session != null) {
				session.setAttribute("accountNumber", accountNumber);
			}
			return txId;
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

	private void checkRoleAuthorization(Role role, Transaction transaction, Account account, Long userId, long branchId)
			throws CustomException {
		if (branchId <= 0) {
			throw new CustomException("Invalid transaction inputs.", HttpStatusCodes.BAD_REQUEST);
		}
		if (role == Role.Employee) {
			if (account.getBranchId() != branchId) {
				logger.warn("Branch ID mismatch for account number: {}", account.getAccountNumber());
				throw new CustomException("Invalid account", HttpStatusCodes.BAD_REQUEST);
			}
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

	public long prepareRecipientTransaction(Transaction transaction, boolean thisBank, Account account, long branchId)
			throws Exception {
		logger.info("Starting transaction processing...");
		Role role = Role.fromString((String) Helper.getThreadLocalValue("role"));
		checkRoleAuthorization(role, transaction, account, transaction.getCustomerId(), branchId);
		ValidationUtil.validateTransactionAmount(transaction.getAmount(), account);
		long txId = createTransaction(transaction, account);

		if (thisBank) {
			processTransactionForThisBank(transaction, txId, account);
		}

		return txId;
	}

	private void processTransactionForThisBank(Transaction transaction, Long txId, Account account) throws Exception {

		Long transactionUserId = account.getUserId();
		updateTransactionDetails(transaction, transactionUserId, txId);

		try {
			long accountNumber = transaction.getAccountNumber();
			account = getAccount(accountNumber);

			BigDecimal closingBalance = computeClosingBalance(transaction.getTransactionTypeEnum(),
					transaction.getAmount(), account);
			transaction.setCustomerId(account.getUserId());
			transaction.setClosingBalance(closingBalance);

			createTransaction(transaction, account);
		} catch (CustomException e) {
			updateTransactionStatus(txId, TransactionStatus.Cancelled);
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
		switch (txType) {
		case Debit:
		case Loan:
			transaction.setTransactionTypeEnum(TransactionType.Credit);
			break;
		case FixedDeposit:
			transaction.setTransactionTypeEnum(TransactionType.FixedDeposit);
			break;
		case Default:
			transaction.setTransactionTypeEnum(TransactionType.Withdraw);
			break;
		default:
			break;
		}
	}

	private void updateAccountBalance(long accountNumber, BigDecimal closingBalance) throws Exception {
		DAO<Account> accountDAO = DaoFactory.getDAO(Account.class);
		Map<String, Object> accountMap = new HashMap<>();
		accountMap.put("accountNumber", accountNumber);

		ColumnCriteria columnCriteria = new ColumnCriteria().setFields(Arrays.asList("balance"))
				.setValues(Arrays.asList(closingBalance));

		accountDAO.update(columnCriteria, accountMap);
	}

	public void updateTransactionStatus(Long txId, TransactionStatus status) throws Exception {
		logger.info("status: " + status);

		ColumnCriteria columnCriteria = new ColumnCriteria().setFields(Arrays.asList("transactionStatus"))
				.setValues(Arrays.asList(status.toString()));

		Map<String, Object> txMap = new HashMap<>();
		txMap.put("id", txId);

		transactionDAO.update(columnCriteria, txMap);
	}

	public void updateTransaction(Long txId) throws Exception {
		Map<String, Object> txMap = new HashMap<>();
		txMap.put("id", txId);
		logger.info("id: " + txId);
		List<Transaction> transactions = transactionDAO.get(txMap);
		if (transactions == null || transactions.isEmpty()) {
			throw new CustomException("Invalid transaction id.", HttpStatusCodes.BAD_REQUEST);
		}

		updateTransactionStatus(txId, TransactionStatus.Completed);
		for (Transaction transaction : transactions) {
			updateAccountBalance(transaction.getAccountNumber(), transaction.getClosingBalance());
		}
	}

	public Long createTransaction(Transaction transaction, Account account) throws Exception {
		logger.info("Creating a new transaction...");
		setInitialTransactionDetails(transaction);
		logger.info(transaction);

		ValidationUtil.validateModel(transaction, Transaction.class);
		long txId = saveTransaction(transaction);
		return txId;
	}

	private void setInitialTransactionDetails(Transaction transaction) {
		if (transaction.getTransactionTypeEnum().equals(TransactionType.FixedDeposit)) {
			transaction.setTransactionStatusEnum(TransactionStatus.Completed);
		} else {
			transaction.setTransactionStatusEnum(TransactionStatus.Pending);
		}
		transaction.setTransactionTime(System.currentTimeMillis());
		transaction.setPerformedBy((Long) Helper.getThreadLocalValue("id"));
	}

	private BigDecimal computeClosingBalance(TransactionType transactionType, BigDecimal amount, Account account)
			throws Exception {
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
		case FixedDeposit:
			return computeFixedDepositBalance(accountBalance, amount, account);
		case Loan:
			LoanService.getInstance().createLoan(account.getAccountNumber(), amount);
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

	private BigDecimal computeFixedDepositBalance(BigDecimal accountBalance, BigDecimal amount, Account account)
			throws CustomException {
		logger.info("Computing fixed deposit balance...");
		if (AccountType.Operational == account.getAccountTypeEnum()) {
			return accountBalance.subtract(amount);
		} else {
			return accountBalance.add(amount);
		}
	}
}