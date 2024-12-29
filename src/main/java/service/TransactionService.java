package service;

import java.util.Map;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
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

	/**
	 * Retrieves a list of transactions based on the provided criteria.
	 * 
	 * @param id            The transaction ID associated with the transactions.
	 * @param accountNumber The account number associated with the transactions.
	 * @param limitValue    The number of transactions.
	 * @param from          The start date for filtering transactions (can be null).
	 * @param to            The end date for filtering transactions (can be null).
	 * @return A list of matching transactions.
	 * @throws CustomException If an error occurs while retrieving transactions.
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> getTransactionDetails(Map<String, Object> txMap) throws CustomException {
		try {
			AccountDAO accountDAO = new AccountDAO();
			Account primaryAccount = null;
			Long id = (Long) txMap.get("id"), accountNumber = (Long) txMap.get("accountNumber");
			if (id == -1l) {
				id = (Long) Helper.getThreadLocalValue().get("id");
			}
			if (accountNumber == null) {
				List<Account> accounts = accountDAO.getAccounts(id, 0l, 0l, 0l, 0l);
				if (accounts == null) {
					logger.error("No Accounts found");
					throw new CustomException("No accounts found for user " + id);
				}
				primaryAccount = accounts.stream().filter(Account::getIsPrimary).findAny().orElse(null);
				if (primaryAccount == null) {
					logger.error("Primary account can't be null");
					throw new CustomException("No primary account found.");
				}
			}
			String role = Helper.getThreadLocalValue().get("role").toString();
			if (role.equals("Employee")) {
				accountNumber = primaryAccount != null ? primaryAccount.getAccountNumber() : accountNumber;
				List<Account> accounts = accountDAO.getAccounts(id, accountNumber, 0L, 0L, 0L);
				txMap.remove("id");
				txMap.remove("accountNumber");
				return transactionDAO.getTransactionByBranchId(accounts, txMap);
			}
			long primaryAccountNumber = primaryAccount != null ? primaryAccount.getAccountNumber() : accountNumber;
			txMap.put("accountNumber", primaryAccountNumber);
			Map<String, Object> txResult = transactionDAO.getTransactions(txMap);
			logger.debug("Retrieved {} transaction(s) for the given criteria",
					((List<Transaction>) txResult.get("transactions")).size());
			return txResult;
		} catch (Exception e) {
			logger.error("Error fetching transaction details: {}", e);
			throw new CustomException(e.getMessage());
		}
	}

	/**
	 * Creates a new transaction.
	 * 
	 * @param transactionMap A map containing transaction details.
	 * @throws CustomException If the transaction creation process fails.
	 */
	public void prepareTransaction(Map<String, Object> transactionMap) throws CustomException {
		logger.info("Initiating transaction creation...");
		AccountService accountDAO = new AccountService();
		List<Account> accounts;

		if (Helper.getThreadLocalValue().get("role").equals("Employee")) {
			long branchId = Long.parseLong((String) transactionMap.get("branchId"));
			long accountNumber = Long.parseLong((String) transactionMap.get("accountNumber"));
			long transactionAccountNumber = Long.parseLong((String) transactionMap.get("transactionAccountNumber"));
			logger.debug("Employee role detected. Verifying branch ID for accounts...");

			// Use cache for account details
			accounts = accountDAO.getAccountDetails(0l, transactionAccountNumber, 0l, 0l);
			Account account = accounts.get(0);
			if (account.getBranchId() != branchId) {
				logger.warn("Branch ID mismatch for account number: {}", accountNumber);
				throw new CustomException("Invalid account");
			}
			accounts = accountDAO.getAccountDetails(0l, transactionAccountNumber, 0l, 0l);
			Account transactionAccount = accounts.get(0);
			if (transactionAccount.getBranchId() != branchId) {
				logger.warn("Branch ID mismatch for transaction account number: {}", transactionAccountNumber);
				throw new CustomException("Invalid account");
			}
		}

		BranchDAO branchDAO = new BranchDAO();
		Long branchId = Long.parseLong((String) transactionMap.get("branchId"));
		List<Branch> branches = branchDAO.getBranch(branchId, false);
		String ifsc = branches.get(0).getIfscCode();
		transactionMap.put("ifsc", ifsc);
		transactionMap.remove("branchId");
		logger.debug("IFSC code retrieved and set for branch ID: {}", branchId);
		if (transactionMap.get("bankName").equals("Horizon")) {
			logger.debug("Bank name is Horizon. Retrieving transaction IFSC...");
			try {
				long transactionAccountNumber = Long.parseLong((String) transactionMap.get("transactionAccountNumber"));
				accounts = accountDAO.getAccountDetails(0l, transactionAccountNumber, 0l, 0l);
				Account transactionAccount = accounts.get(0);
				branches = branchDAO.getBranch(transactionAccount.getBranchId(), false);
				String transactionIfsc = branches.get(0).getIfscCode();
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
				List<Account> accounts = accountDAO.getAccounts(0l, transactionAccountNumber, 0l, 0l, 0l);
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

	public Long createTransaction(Transaction transaction) throws CustomException {
		logger.info("Creating a new transaction...");

		AccountDAO accountDAO = new AccountDAO();
		transaction.setStatus("Completed");
		transaction.setTransactionTime(System.currentTimeMillis());
		transaction.setPerformedBy((Long) Helper.getThreadLocalValue().get("id"));

		String transactionType = transaction.getTransactionType();
		BigDecimal amount = transaction.getAmount();
		List<Account> accounts = accountDAO.getAccounts(0l, transaction.getAccountNumber(), 0l, 0l, 0l);
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

	private BigDecimal computeDepositBalance(BigDecimal accountBalance, Transaction transaction, AccountDAO accountDAO)
			throws CustomException {
		logger.info("Computing deposit balance...");
		Account account = accountDAO.getAccounts(0l, transaction.getAccountNumber(), 0l, 0l, 0l).get(0);
		if (!"Operational".equals(account.getAccountType())) {
			return accountBalance.add(transaction.getAmount());
		} else {
			return accountBalance.subtract(transaction.getAmount());
		}
	}

}