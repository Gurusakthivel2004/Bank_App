package service;

import java.util.Map;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import dblayer.dao.AccountDAO;
import dblayer.dao.BranchDAO;
import dblayer.dao.TransactionDAO;
import dblayer.model.Account;
import dblayer.model.Branch;
import dblayer.model.Criteria;
import dblayer.model.Transaction;
import util.CustomException;
import util.Helper;

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
			Map<String, Object> txResult = transactionDAO.getTransactions(txMap);
			return txResult;

		} catch (Exception e) {
			logger.error("Error fetching transaction details: {}", e);
			throw new CustomException("Error fetching transaction details: " + e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	private Account fetchPrimaryAccount(Long customerId) throws CustomException {
		Map<String, Object> accountMap = new HashMap<>();
		accountMap.put("userId", customerId);
		List<Account> accounts = (List<Account>) new AccountDAO().getAccounts(accountMap).get("accounts");
		if (accounts == null || accounts.isEmpty()) {
			logger.error("No Accounts found for user {}", customerId);
			throw new CustomException("No accounts found for user " + customerId);
		}
		return accounts.stream().filter(Account::getIsPrimary).findFirst()
				.orElseThrow(() -> new CustomException("No primary account found for user " + customerId));
	}

	@SuppressWarnings("unchecked")
	public void prepareTransaction(Map<String, Object> transactionMap) throws CustomException {
		logger.info("Initiating transaction creation...");
		AccountService accountDAO = new AccountService();
		List<Account> accounts;
		Map<String, Object> accountMap = new HashMap<>();
		if (Helper.getThreadLocalValue().get("role").equals("Employee")) {
			long branchId = Long.parseLong((String) transactionMap.get("branchId"));
			long accountNumber = Long.parseLong((String) transactionMap.get("accountNumber"));
			long transactionAccountNumber = Long.parseLong((String) transactionMap.get("transactionAccountNumber"));
			logger.debug("Employee role detected. Verifying branch ID for accounts...");

			// Use cache for account details

			accountMap.put("accountNumber", accountNumber);
			accounts = (List<Account>) accountDAO.getAccountDetails(accountMap).get("accounts");
			Account account = accounts.get(0);
			if (account.getBranchId() != branchId) {
				logger.warn("Branch ID mismatch for account number: {}", accountNumber);
				throw new CustomException("Invalid account");
			}
			accountMap.put("accountNumber", transactionAccountNumber);
			accounts = (List<Account>) accountDAO.getAccountDetails(accountMap).get("accounts");
			Account transactionAccount = accounts.get(0);
			if (transactionAccount.getBranchId() != branchId) {
				logger.warn("Branch ID mismatch for transaction account number: {}", transactionAccountNumber);
				throw new CustomException("Invalid account");
			}
		}

		BranchDAO branchDAO = new BranchDAO();
		Long branchId = Long.parseLong((String) transactionMap.get("branchId"));
		List<Object> branches = branchDAO.getBranch(branchId, false);
		String ifsc = ((Branch) branches.get(0)).getIfscCode();
		transactionMap.put("ifsc", ifsc);
		transactionMap.remove("branchId");
		logger.debug("IFSC code retrieved and set for branch ID: {}", branchId);
		if (transactionMap.get("bankName").equals("Horizon")) {
			logger.debug("Bank name is Horizon. Retrieving transaction IFSC...");
			try {
				long transactionAccountNumber = Long.parseLong((String) transactionMap.get("transactionAccountNumber"));
				accountMap.put("accountNumber", transactionAccountNumber);
				accounts = (List<Account>) accountDAO.getAccountDetails(accountMap).get("accounts");
				Account transactionAccount = accounts.get(0);
				branches = branchDAO.getBranch(transactionAccount.getBranchId(), false);
				String transactionIfsc = ((Branch) branches.get(0)).getIfscCode();
				transactionMap.put("transactionIfsc", transactionIfsc);
			} catch (IndexOutOfBoundsException e) {
				logger.error("Error while fetching transaction account details: {}", e.getMessage());
				throw new CustomException("Enter valid credentials");
			}
		}

		transactionMap.put("customerId", (Long) Helper.getThreadLocalValue().get("id"));
		Transaction transaction = Helper.createPojoFromMap(transactionMap, Transaction.class);
		logger.debug("Validating the transaction object...");
		logger.info("Transaction object validation passed. Proceeding with transaction creation...");
		// Make the transaction and invalidate cache
		prepareRecipientTransaction(transaction, transaction.getBankName().equals("Horizon"));
		logger.info("Transaction successfully created for account number: {}", transaction.getAccountNumber());
	}

	@SuppressWarnings("unchecked")
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
				List<Account> accounts = (List<Account>) accountDAO.getAccounts(accountMap).get("accounts");
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
					transaction.setTransactionType("Credit");
				}

				createTransaction(transaction);
			} catch (Exception e) {
				logger.error("Error during intra-bank transaction, rolling back...", e);
				Criteria criteria = new Criteria();
				criteria.setClazz(Transaction.class);
				criteria.setColumn(new ArrayList<>(Arrays.asList("id")));
				criteria.setOperator(new ArrayList<>(Arrays.asList("=")));
				criteria.setValue(new ArrayList<>(Arrays.asList(txId)));
				transactionDAO.removeFailedTransaction(criteria);
				throw new CustomException(e.getMessage());
			}
		}
	}

	@SuppressWarnings("unchecked")
	public Long createTransaction(Transaction transaction) throws CustomException {
		logger.info("Creating a new transaction...");

		AccountDAO accountDAO = new AccountDAO();
		transaction.setStatus("Completed");
		transaction.setTransactionTime(System.currentTimeMillis());
		transaction.setPerformedBy((Long) Helper.getThreadLocalValue().get("id"));

		String transactionType = transaction.getTransactionType();
		BigDecimal amount = transaction.getAmount();
		Map<String, Object> accountMap = new HashMap<>();
		accountMap.put("accountNumber", transaction.getAccountNumber());
		List<Account> accounts = (List<Account>) accountDAO.getAccounts(accountMap).get("accounts");
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
			throw new CustomException("Invalid transaction type: " + transactionType);
		}

		transaction.setClosingBalance(closingBalance);
		logger.info("Transaction details: " + transaction);
		Helper.validateModel(transaction);
		Long txId = transactionDAO.createTransaction(transaction);
		logger.info("Transaction created with ID: " + txId);

		transactionDAO.updateAccountBalance(transaction.getAccountNumber(), closingBalance);
		return txId;
	}

	@SuppressWarnings("unchecked")
	private BigDecimal computeDepositBalance(BigDecimal accountBalance, Transaction transaction, AccountDAO accountDAO)
			throws CustomException {
		logger.info("Computing deposit balance...");
		Map<String, Object> accountMap = new HashMap<>();
		accountMap.put("accountNumber", transaction.getAccountNumber());
		List<Account> accounts = (List<Account>) accountDAO.getAccounts(accountMap).get("accounts");
		Account account = accounts.get(0);
		if (!"Operational".equals(account.getAccountType())) {
			return accountBalance.add(transaction.getAmount());
		} else {
			return accountBalance.subtract(transaction.getAmount());
		}
	}

}