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

			if (customerId != null && customerId == -1L) {
				customerId = (Long) Helper.getThreadLocalValue("id");
				txMap.put("customerId", customerId);

				if (accountNumber == 0L) {
					Account primaryAccount = fetchPrimaryAccount(customerId);
					accountNumber = primaryAccount != null ? primaryAccount.getAccountNumber() : accountNumber;
				}
			}
			Role role = Role.fromString((String) Helper.getThreadLocalValue("role"));
			if (role == Role.Employee) {
				txMap.put("branchId", Helper.getThreadLocalValue("branchId"));
			}

			txMap.put("accountNumber", accountNumber);

			Map<String, Object> txResult = new HashMap<>();
			Long offset = (Long) txMap.getOrDefault("offset", -1l);
			if (offset == 0) {
				Long count = transactionDAO.getDataCount(txMap);
				txResult.put("count", count);
			}
			List<Transaction> transactions = transactionDAO.get(txMap);

			AuthorizationService authService = new AuthorizationService();
			if (!authService.isAuthorized("transaction", transactions)) {
				throw new CustomException("Not authorized to access transaction details", HttpStatusCodes.UNAUTHORIZED);
			}

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

	public void prepareTransaction(Map<String, Object> transactionMap) throws CustomException {

		logger.info("Initiating transaction creation...");
		List<Account> accounts;
		Map<String, Object> accountMap = new HashMap<>();
		long branchId = Long.parseLong((String) transactionMap.get("branchId"));
		long accountNumber = Long.parseLong((String) transactionMap.get("accountNumber"));
		long transactionAccountNumber = Long.parseLong((String) transactionMap.get("transactionAccountNumber"));
		Long userId = (Long) Helper.getThreadLocalValue("id");
		Role role = Role.fromString((String) Helper.getThreadLocalValue("role"));

		accountMap.put("accountNumber", accountNumber);
		TransactionType transactionType = TransactionType.fromString((String) transactionMap.get("transactionType"));
		if (transactionType == TransactionType.Withdraw) {
			accountMap.put("accountNumber", transactionAccountNumber);
			transactionMap.put("transactionType", "Default");
		}
		accounts = new AccountDAO().get(accountMap);
		Account account = accounts.get(0);
		long customerId = account.getUserId();
		try {
			double amount = Double.parseDouble((String) transactionMap.get("amount"));
			BigDecimal transactionAmount = BigDecimal.valueOf(amount);
			transactionMap.put("amount", transactionAmount);

			if (transactionAmount.compareTo(BigDecimal.ZERO) <= 0) {
				throw new CustomException("Enter an amount greater than 0.", HttpStatusCodes.BAD_REQUEST);
			}

			BigDecimal accountBalance = account.getBalance();
			if (accountBalance.compareTo(transactionAmount) < 0) {
				throw new CustomException("Insufficient balance.", HttpStatusCodes.BAD_REQUEST);
			}

		} catch (NumberFormatException e) {
			throw new CustomException("Invalid amount format", HttpStatusCodes.BAD_REQUEST);
		}
		accountMap.put("accountNumber", accountNumber);
		if (transactionMap.get("transactionType").equals("Default")) {
			accounts = new AccountDAO().get(accountMap);
			account = accounts.get(0);
			customerId = account.getUserId();
		}
		if (role == Role.Employee) {
			if (!(transactionMap.get("type") == "Debit") && account.getUserId() != userId) {
				throw new CustomException("Unauthorized account found", HttpStatusCodes.UNAUTHORIZED);
			}
			if (account.getBranchId() != branchId) {
				logger.warn("Branch ID mismatch for account number: {}", accountNumber);
				throw new CustomException("Invalid account", HttpStatusCodes.BAD_REQUEST);
			}
		}
		if (role == Role.Customer && account.getUserId() != userId) {
			throw new CustomException("Unauthorized account found", HttpStatusCodes.UNAUTHORIZED);
		}

		DAO<Branch> branchDAO = new BranchDAO();
		Map<String, Object> branchMap = new HashMap<>();
		branchMap.put("notExact", false);
		branchMap.put("branchId", branchId);
		List<Branch> branches = branchDAO.get(branchMap);
		String ifsc = branches.get(0).getIfscCode();
		transactionMap.put("ifsc", ifsc);
		transactionMap.remove("branchId");
		logger.debug("IFSC code retrieved and set for branch ID: {}", branchId);
		if (transactionMap.get("bankName").equals("Horizon")) {
			logger.debug("Bank name is Horizon. Retrieving transaction IFSC...");
			try {
				accountMap.put("accountNumber", transactionAccountNumber);
				accounts = new AccountDAO().get(accountMap);
				Account transactionAccount = accounts.get(0);

				if (transactionAccount == null) {
					throw new CustomException("Transaction Account does not exists", HttpStatusCodes.BAD_REQUEST);
				}

				branchMap.put("branchId", transactionAccount.getBranchId());

				branches = branchDAO.get(branchMap);
				String transactionIfsc = ((Branch) branches.get(0)).getIfscCode();
				transactionMap.put("transactionIfsc", transactionIfsc);
			} catch (IndexOutOfBoundsException e) {
				logger.error("Error while fetching transaction account details: {}", e.getMessage());
				throw new CustomException("Enter valid credentials", HttpStatusCodes.BAD_REQUEST);
			}
		}

		transactionMap.put("customerId", customerId);
		Transaction transaction = Helper.createPojoFromMap(transactionMap, Transaction.class);
		logger.info("Transaction object validation passed. Proceeding with transaction creation...");
		// Make the transaction and invalidate cache
		prepareRecipientTransaction(transaction, transaction.getBankName().equals("Horizon"));
		logger.info("Transaction successfully created for account number: {}", transaction.getAccountNumber());

	}

	public void prepareRecipientTransaction(Transaction transaction, boolean thisBank) throws CustomException {
		logger.info("Starting transaction processing...");
		Long txId = createTransaction(transaction);
		if (thisBank) {
			AccountDAO accountDAO = new AccountDAO();
			Long accountNumber = transaction.getAccountNumber();
			Long transactionAccountNumber = transaction.getTransactionAccountNumber();
			String transactionIfsc = transaction.getTransactionIfsc();
			String ifsc = transaction.getIfsc();
			Map<String, Object> accountMap = new HashMap<>();
			accountMap.put("accountNumber", transactionAccountNumber);
			List<Account> accounts = accountDAO.get(accountMap);
			Helper.checkNullValues(accounts);

			TransactionType txType = transaction.getTransactionTypeEnum();
			Long transactionUserId = accounts.get(0).getUserId();

			transaction.setIfsc(transactionIfsc);
			transaction.setTransactionIfsc(ifsc);
			transaction.setAccountNumber(transactionAccountNumber);
			transaction.setTransactionAccountNumber(accountNumber);
			transaction.setCustomerId(transactionUserId);
			transaction.setId(txId);

			if (txType == TransactionType.Debit) {
				transaction.setTransactionTypeEnum(TransactionType.Credit);
			} else if (txType == TransactionType.Default) {
				transaction.setTransactionTypeEnum(TransactionType.Withdraw);
			}
			try {
				createTransaction(transaction);
			} catch (Exception e) {
				ColumnCriteria columnCriteria = new ColumnCriteria().setFields(Arrays.asList("status"))
						.setValues(Arrays.asList(TransactionStatus.Cancelled));
				Map<String, Object> txMap = new HashMap<String, Object>();
				txMap.put("id", txId);
				transactionDAO.update(columnCriteria, txMap);
			}
		}
	}

	public Long createTransaction(Transaction transaction) throws CustomException {
		try {
			logger.info("Creating a new transaction...");

			AccountDAO accountDAO = new AccountDAO();
			transaction.setTransactionStatusEnum(TransactionStatus.Completed);
			transaction.setTransactionTime(System.currentTimeMillis());
			transaction.setPerformedBy((Long) Helper.getThreadLocalValue("id"));

			BigDecimal amount = transaction.getAmount();
			Map<String, Object> accountMap = new HashMap<>();
			accountMap.put("accountNumber", transaction.getAccountNumber());
			List<Account> accounts = accountDAO.get(accountMap);
			Helper.checkNullValues(accounts);

			BigDecimal accountBalance = accounts.get(0).getBalance();
			BigDecimal closingBalance;

			TransactionType transactionType = transaction.getTransactionTypeEnum();

			System.out.println(transaction.getAccountNumber() + " " + transactionType.toString());
			switch (transactionType) {
			case Credit:
				closingBalance = accountBalance.add(amount);
				break;
			case Withdraw:
			case Debit:
				closingBalance = accountBalance.subtract(amount);
				break;
			case Deposit:
				closingBalance = computeDepositBalance(accountBalance, transaction, accountDAO);
				break;
			case Default:
				closingBalance = accountBalance;
				transaction.setTransactionTypeEnum(TransactionType.Withdraw);
				break;
			default:
				logger.error("Invalid transaction type: " + transactionType);
				throw new CustomException("Invalid transaction type: " + transactionType, HttpStatusCodes.BAD_REQUEST);
			}
			transaction.setClosingBalance(closingBalance);
			logger.info("Transaction details: " + transaction);
			ValidationUtil.validateModel(transaction, Transaction.class);

			Long txId = transactionDAO.create(transaction);
			logger.info("Transaction created with ID: " + txId);

			ColumnCriteria columnCriteria = new ColumnCriteria().setFields(Arrays.asList("balance"))
					.setValues(Arrays.asList(closingBalance));

			accountDAO.update(columnCriteria, accountMap);

			CacheUtil cacheUtil = new CacheUtil();
			String accountkey = "accountInfo" + (transaction.getAccountNumber() % 10000);
			cacheUtil.delete(accountkey);

			ActivityLog activityLog = new ActivityLog().setLogMessage("Transaction created").setLogType(LogType.Insert)
					.setUserAccountNumber(transaction.getAccountNumber()).setRowId(txId).setTableName("Transaction")
					.setUserId(transaction.getCustomerId());

			TaskExecutorService.getInstance().submit(activityLog);

			return txId;
		} catch (Exception e) {
			logger.error("Unexpected error occurred while preparing transaction: {}", e.getMessage());
			throw new CustomException("Error occured while processing. Please check your inputs.",
					HttpStatusCodes.BAD_REQUEST);
		}
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