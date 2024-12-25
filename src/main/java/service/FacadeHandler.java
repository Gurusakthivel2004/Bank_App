package service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import dblayer.model.Account;
import dblayer.model.Branch;
import dblayer.model.Transaction;
import util.CustomException;
import util.Helper;

public class FacadeHandler {

	private final Logger logger = LogManager.getLogger(FacadeHandler.class);
	private TransactionService transactionService = new TransactionService();
	private UserService userService = new UserService();
	private AccountService accountService = new AccountService();

	/**
	 * Fetches the dashboard details for the user based on their role.
	 * 
	 * @return A map containing dashboard details including transactions, accounts,
	 *         user details, and branches.
	 * @throws CustomException If there is any error while fetching the dashboard
	 *                         details.
	 */
	public Map<String, Object> dashBoardDetails() throws CustomException {
		long id = (Long) Helper.getThreadLocalValue().get("id");
		Map<String, Object> map = new HashMap<>();

		logger.info("Fetching dashboard details for user ID: {}", id);

		try {
			logger.debug("Fetching account details for user ID: {}", id);
			List<Account> accounts = accountService.getAccountDetails(id, 0l, 0l, 0l);
			map.put("account", accounts);
			logger.info(accounts);
			// Fetch transaction details
			List<Transaction> transactions = new ArrayList<>();
			for (int i = 0; i < accounts.size(); i++) {
				Account account = accounts.get(i);
				List<Transaction> accountTransactions = transactionService.getTransactionDetails(id,
						account.getAccountNumber(), 5L, 0L, 0L);
				transactions.addAll(accountTransactions);
			}

			logger.debug("Fetching transaction details for user ID: {}", id);
			map.put("transactions", transactions);

			// Fetch user details based on role
			String role = (String) Helper.getThreadLocalValue().get("role");
			String key = "Customer".equals(role) ? "customerDetail" : "staff";
			logger.debug("Fetching {} details for user ID: {}", key, id);
			map.put(key, userService.getUserDetails(id, role));

			// Fetch branch details for the accounts
			logger.debug("Fetching branch details for user ID: {}", id);
			List<Branch> branchDetails = new ArrayList<>();
			BranchService branchService = new BranchService();
			for (Account account : accounts) {
				Branch branch = branchService.getBranchDetails(account.getBranchId()).get(0);
				if (!branchDetails.contains(branch)) {
					branchDetails.add(branch);
				}
			}
			map.put("branch", branchDetails);

			logger.debug("Adding monthly finance details for user ID: {}", id);
			addMonthlyFinance(map, id, 3);

			logger.info("Dashboard details fetched successfully for user ID: {}", id);
		} catch (CustomException e) {
			logger.error("Error occurred while fetching dashboard details for user ID: {}: {}", id, e.getMessage());
			throw e;
		} catch (Exception e) {
			logger.error("Unexpected error occurred while fetching dashboard details for user ID: {}: {}", id,
					e.getMessage(), e);
			throw new CustomException("An unexpected error occurred while fetching details.");
		}

		return map;
	}

	/**
	 * Adds monthly finance details to the provided map.
	 * 
	 * @param map        The map to which monthly finance data will be added.
	 * @param customerId The ID of the customer for whom the finance details are
	 *                   fetched.
	 * @throws CustomException If there is an error while fetching the monthly
	 *                         finance details.
	 */
	private void addMonthlyFinance(Map<String, Object> map, Long customerId, int monthLength) throws CustomException {
		int month = LocalDate.now().getMonthValue();
		int year = LocalDate.now().getYear();

		logger.debug("Adding monthly finance details for customer ID: {}", customerId);
		for (int i = 0; i < monthLength; i++) {
			int currMonth = month - i;
			long startMillis = Helper.getStartOfMonthMillis(year, currMonth);
			Long endMillis = Helper.getEndOfMonthMillis(year, currMonth);

			logger.debug("Fetching transaction details for month: {} (Start: {}, End: {})", currMonth, startMillis,
					endMillis);
			List<Transaction> transactions = transactionService.getTransactionDetails(customerId, 0l, 0l, startMillis,
					endMillis);

			Map<String, BigDecimal> monthlyFinance = new HashMap<>();
			for (Transaction tx : transactions) {
				String type = tx.getTransactionType();
				monthlyFinance.put(type,
						((BigDecimal) monthlyFinance.getOrDefault(type, new BigDecimal(0))).add(tx.getAmount()));
			}

			map.put(currMonth + "", monthlyFinance);
		}

		logger.info("Monthly finance details added successfully for customer ID: {}", customerId);
	}
}
