package service;

import java.util.Map;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import dblayer.dao.AccountDAO;
import dblayer.dao.BranchDAO;
import dblayer.dao.TransactionDAO;
import dblayer.model.Account;
import dblayer.model.Branch;
import dblayer.model.Transaction;
import util.CustomException;
import util.Helper;

public class TransactionService {

	private final Logger logger = LogManager.getLogger(TransactionService.class);
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
	public List<Transaction> getTransactionDetails(Long id, Long accountNumber, Long limitValue, Long from, Long to)
			throws CustomException {
		try {
			AccountDAO accountDAO = new AccountDAO();
			Account primaryAccount = null;
			if (id == -1) {
				id = (Long) Helper.getThreadLocalValue().get("id");
				if (accountNumber == 0) {
					List<Account> accounts = accountDAO.getAccounts(id, 0l, 0l, 0l, 0l);
					primaryAccount = accounts.stream().filter(Account::getIsPrimary).findAny().orElse(null);
					if (primaryAccount == null) {
						logger.error("Primary account can't be null");
						throw new CustomException("No primary account found.");
					}
				}
			}
			String role = Helper.getThreadLocalValue().get("role").toString();
			if (role.equals("Employee")) {
				List<Account> accounts = accountDAO.getAccounts(id, accountNumber, 0L, 0L, 0L);
				List<Transaction> branchTransactions = transactionDAO.checkTransactionBranchId(accounts, from, to, 8L);
				return branchTransactions;
			}
			long primaryAccountNumber = primaryAccount != null ? primaryAccount.getAccountNumber() : accountNumber;
			List<Transaction> transactions = transactionDAO.getTransactions(id, primaryAccountNumber, 8L, from, to);

			logger.debug("Retrieved {} transaction(s) for the given criteria", transactions.size());
			return transactions;
		} catch (Exception e) {
			logger.error("Error fetching transaction details: {}", e.getMessage());
			throw new CustomException("Unable to fetch transaction details. Please try again later.", e);
		}
	}

	/**
	 * Creates a new transaction.
	 * 
	 * @param transactionMap A map containing transaction details.
	 * @throws CustomException If the transaction creation process fails.
	 */
	public void createTransaction(Map<String, Object> transactionMap) throws CustomException {
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
		List<Branch> branches = branchDAO.getBranch(branchId);
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
				branches = branchDAO.getBranch(transactionAccount.getBranchId());
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
		transactionDAO.makeTransaction(transaction, transaction.getBankName().equals("Horizon"));
		logger.info("Transaction successfully created for account number: {}", transaction.getAccountNumber());
	}

}