package service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import Enum.Constants.HttpStatusCodes;
import Enum.Constants.TransactionStatus;
import Enum.Constants.TransactionType;
import dao.AccountDAO;
import dao.BranchDAO;
import dao.TransactionDAO;
import model.Account;
import model.Branch;
import model.Transaction;
import util.CustomException;
import util.Helper;
import util.ValidationUtil;

public class TransactionService {

	private final Logger logger = LogManager.getLogger(TransactionService.class.getName());
	private TransactionDAO transactionDAO = new TransactionDAO();

	public Map<String, Object> getTransactionDetails(Map<String, Object> txMap) throws CustomException {
		try {
			Long customerId = (Long) txMap.get("customerId");
			Long accountNumber = (Long) txMap.getOrDefault("accountNumber", 0L);
			if (customerId != null && customerId == -1L) {
				customerId = (Long) Helper.getThreadLocalValue().get("id");
				txMap.put("customerId", customerId);

				if (accountNumber == 0L) {
					Account primaryAccount = fetchPrimaryAccount(customerId);
					accountNumber = primaryAccount != null ? primaryAccount.getAccountNumber() : accountNumber;
				}
			}
			String role = Helper.getThreadLocalValue().get("role").toString();
			if ("Employee".equals(role)) {
				txMap.put("branchId", Helper.getThreadLocalValue().get("branchId"));
			}

			txMap.put("accountNumber", accountNumber);

			Map<String, Object> txResult = new HashMap<>();
			Long offset = (Long) txMap.getOrDefault("offset", -1l);
			if (offset == 0) {
				Long count = transactionDAO.getDataCount(txMap);
				txResult.put("count", count);
			}
			List<Transaction> transactions = transactionDAO.getTransactions(txMap);
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
		List<Account> accounts = new AccountDAO().getAccounts(accountMap);
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
		logger.debug("Employee role detected. Verifying branch ID for accounts...");

		accountMap.put("accountNumber", accountNumber);
		accounts = new AccountDAO().getAccounts(accountMap);
		Account account = accounts.get(0);
		long customerId = account.getUserId();
		try {
			String amountStr = transactionMap.get("amount").toString();
			BigDecimal transactionAmount = new BigDecimal(amountStr);

			if (account.getBalance().compareTo(transactionAmount) < 0) {
				throw new CustomException("Insufficient balance", HttpStatusCodes.BAD_REQUEST);
			}
		} catch (NumberFormatException e) {
			throw new CustomException("Invalid amount format", HttpStatusCodes.BAD_REQUEST);
		}
		if (Helper.getThreadLocalValue().get("role").equals("Employee")) {
			if (account.getBranchId() != branchId) {
				logger.warn("Branch ID mismatch for account number: {}", accountNumber);
				throw new CustomException("Invalid account", HttpStatusCodes.BAD_REQUEST);
			}
		}
		Long id = (Long) Helper.getThreadLocalValue().get("id");
		String role = (String) Helper.getThreadLocalValue().get("rolw");
		if (role.equals("Customer") && account.getUserId() != id) {
			throw new CustomException("Unauthorized account found", HttpStatusCodes.UNAUTHORIZED);
		}

		BranchDAO branchDAO = new BranchDAO();
		List<Branch> branches = branchDAO.getBranch(branchId, false);
		String ifsc = branches.get(0).getIfscCode();
		transactionMap.put("ifsc", ifsc);
		transactionMap.remove("branchId");
		logger.debug("IFSC code retrieved and set for branch ID: {}", branchId);
		if (transactionMap.get("bankName").equals("Horizon")) {
			logger.debug("Bank name is Horizon. Retrieving transaction IFSC...");
			try {
				long transactionAccountNumber = Long.parseLong((String) transactionMap.get("transactionAccountNumber"));
				accountMap.put("accountNumber", transactionAccountNumber);
				accounts = new AccountDAO().getAccounts(accountMap);
				Account transactionAccount = accounts.get(0);
				branches = branchDAO.getBranch(transactionAccount.getBranchId(), false);
				String transactionIfsc = ((Branch) branches.get(0)).getIfscCode();
				transactionMap.put("transactionIfsc", transactionIfsc);
			} catch (IndexOutOfBoundsException e) {
				logger.error("Error while fetching transaction account details: {}", e.getMessage());
				throw new CustomException("Enter valid credentials", HttpStatusCodes.BAD_REQUEST);
			}
		}

		transactionMap.put("customerId", customerId);
		Transaction transaction = Helper.createPojoFromMap(transactionMap, Transaction.class);
		logger.debug("Validating the transaction object...");
		logger.info("Transaction object validation passed. Proceeding with transaction creation...");
		// Make the transaction and invalidate cache
		prepareRecipientTransaction(transaction, transaction.getBankName().equals("Horizon"));
		logger.info("Transaction successfully created for account number: {}", transaction.getAccountNumber());
	}

	public void prepareRecipientTransaction(Transaction transaction, boolean thisBank) throws CustomException {
		logger.info("Starting transaction processing...");
		Long txId = createTransaction(transaction);
		if (thisBank) {
			try {
				AccountDAO accountDAO = new AccountDAO();
				Long accountNumber = transaction.getAccountNumber();
				Long transactionAccountNumber = transaction.getTransactionAccountNumber();
				String transactionIfsc = transaction.getTransactionIfsc();
				String ifsc = transaction.getIfsc();
				Map<String, Object> accountMap = new HashMap<>();
				accountMap.put("accountNumber", transactionAccountNumber);
				List<Account> accounts = accountDAO.getAccounts(accountMap);
				Helper.checkNullValues(accounts);

				String transactionType = transaction.getTransactionType();
				Long transactionUserId = accounts.get(0).getUserId();

				transaction.setIfsc(transactionIfsc);
				transaction.setTransactionIfsc(ifsc);
				transaction.setAccountNumber(transactionAccountNumber);
				transaction.setTransactionAccountNumber(accountNumber);
				transaction.setCustomerId(transactionUserId);
				transaction.setId(txId);

				if ("Debit".equals(transactionType)) {
					transaction.setTransactionTypeEnum(TransactionType.Credit);
				}

				createTransaction(transaction);
			} catch (Exception e) {
				logger.error("Error during intra-bank transaction, rolling back...", e);
				throw new CustomException("Error occurred during transaction.", HttpStatusCodes.INTERNAL_SERVER_ERROR);
			}
		}
	}

	public Long createTransaction(Transaction transaction) throws CustomException {
		logger.info("Creating a new transaction...");

		AccountDAO accountDAO = new AccountDAO();
		transaction.setTransactionStatusEnum(TransactionStatus.Completed);
		transaction.setTransactionTime(System.currentTimeMillis());
		transaction.setPerformedBy((Long) Helper.getThreadLocalValue().get("id"));

		String transactionType = transaction.getTransactionType();
		BigDecimal amount = transaction.getAmount();
		Map<String, Object> accountMap = new HashMap<>();
		accountMap.put("accountNumber", transaction.getAccountNumber());
		List<Account> accounts = accountDAO.getAccounts(accountMap);
		Helper.checkNullValues(accounts);

		BigDecimal accountBalance = accounts.get(0).getBalance();
		BigDecimal closingBalance;

		switch (transactionType) {
		case "Credit":
			closingBalance = accountBalance.add(amount);
			break;
		case "Withdraw":
		case "Debit":
			closingBalance = accountBalance.subtract(amount);
			break;
		case "Deposit":
			closingBalance = computeDepositBalance(accountBalance, transaction, accountDAO);
			break;
		default:
			logger.error("Invalid transaction type: " + transactionType);
			throw new CustomException("Invalid transaction type: " + transactionType, HttpStatusCodes.BAD_REQUEST);
		}

		transaction.setClosingBalance(closingBalance);
		logger.info("Transaction details: " + transaction);
		ValidationUtil.validateModel(transaction, Transaction.class);
		Long txId = transactionDAO.createTransaction(transaction);
		logger.info("Transaction created with ID: " + txId);

		transactionDAO.updateAccountBalance(transaction.getAccountNumber(), closingBalance);
		return txId;
	}

	private BigDecimal computeDepositBalance(BigDecimal accountBalance, Transaction transaction, AccountDAO accountDAO)
			throws CustomException {
		logger.info("Computing deposit balance...");
		Map<String, Object> accountMap = new HashMap<>();
		accountMap.put("accountNumber", transaction.getAccountNumber());
		List<Account> accounts = accountDAO.getAccounts(accountMap);
		Account account = accounts.get(0);
		if (!"Operational".equals(account.getAccountType())) {
			return accountBalance.add(transaction.getAmount());
		} else {
			return accountBalance.subtract(transaction.getAmount());
		}
	}

}