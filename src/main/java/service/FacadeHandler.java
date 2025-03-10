package service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import model.Account;
import model.Branch;
import model.Transaction;
import util.Helper;

public class FacadeHandler {

	private static Logger logger = LogManager.getLogger(FacadeHandler.class);
	private static TransactionService transactionService = TransactionService.getInstance();
	private static UserService userService = UserService.getInstance();
	private static AccountService accountService = AccountService.getInstance();
	private static BranchService branchService = BranchService.getInstance();

	public Map<String, Object> dashBoardDetails() throws Exception {
		long userId = (Long) Helper.getThreadLocalValue("id");
		logger.info("Fetching dashboard details for user ID: {}", userId);

		// Fetch user accounts
		List<Account> accounts = getUserAccounts(userId);
		List<Transaction> transactions = new ArrayList<>();
		List<Branch> branchDetails = new ArrayList<>();

		// Fetch transactions and branches for all accounts
		for (Account account : accounts) {
			transactions.addAll(fetchTransactions(userId, account.getAccountNumber()));
			branchDetails.add(fetchBranch(account.getBranchId()));
		}

		// Fetch user details
		String role = (String) Helper.getThreadLocalValue("role");
		Map<String, Object> userDetails = getUserDetails(userId, role);

		// Prepare final response
		Map<String, Object> dashboardData = new HashMap<>();
		dashboardData.put("account", accounts);
		dashboardData.put("transactions", transactions);
		dashboardData.put("branch", branchDetails);
		dashboardData.put("userDetail", userDetails.get("users"));

		// Add monthly finance details
		addMonthlyFinance(dashboardData, userId, 3);

		logger.info("Dashboard details fetched successfully for user ID: {}", userId);
		return dashboardData;
	}

	@SuppressWarnings("unchecked")
	private List<Account> getUserAccounts(long userId) throws Exception {
		Map<String, Object> params = new HashMap<>();
		params.put("userId", userId);
		return (List<Account>) accountService.getAccountDetails(params).get("accounts");
	}

	@SuppressWarnings("unchecked")
	private List<Transaction> fetchTransactions(long userId, long accountNumber) throws Exception {
		Map<String, Object> txParams = new HashMap<>();
		getTransactionMap(txParams, userId, accountNumber, 5L, 0L, 0L, -1L);
		return (List<Transaction>) transactionService.getTransactionDetails(txParams).get("transactions");
	}

	private Branch fetchBranch(long branchId) throws Exception {
		Map<String, Object> branchParams = new HashMap<>();
		branchParams.put("branchId", branchId);
		return branchService.getBranchDetails(branchParams).get(0);
	}

	private Map<String, Object> getUserDetails(long userId, String role) throws Exception {
		Map<String, Object> params = new HashMap<>();
		params.put("role", role);
		params.put("userId", userId);
		return userService.getUserDetails(params);
	}

	@SuppressWarnings("unchecked")
	private void addMonthlyFinance(Map<String, Object> map, Long customerId, int monthLength) throws Exception {
		int currentMonth = LocalDate.now().getMonthValue();
		int currentYear = LocalDate.now().getYear();
		logger.info("Adding monthly finance details for customer ID: {}", customerId);

		for (int i = 0; i < monthLength; i++) {
			int adjustedMonth = currentMonth - i;
			int adjustedYear = currentYear;

			if (adjustedMonth <= 0) {
				adjustedMonth += 12;
				adjustedYear--;
			}

			long startMillis = Helper.getStartOfMonthMillis(adjustedYear, adjustedMonth);
			long endMillis = Helper.getEndOfMonthMillis(adjustedYear, adjustedMonth);

			logger.info("Fetching transaction details for month: {}-{} (Start: {}, End: {})", adjustedYear,
					adjustedMonth, startMillis, endMillis);
			Map<String, Object> txMap = new HashMap<>();
			getTransactionMap(txMap, customerId, 0l, 0L, startMillis, endMillis, -1L);
			List<Transaction> transactions = (List<Transaction>) transactionService.getTransactionDetails(txMap)
					.get("transactions");
			Map<String, BigDecimal> monthlyFinance = new HashMap<>();
			for (Transaction tx : transactions) {
				String type = tx.getTransactionType();
				monthlyFinance.put(type, monthlyFinance.getOrDefault(type, BigDecimal.ZERO).add(tx.getAmount()));
			}
			map.put(adjustedMonth + "", monthlyFinance);
		}

		logger.info("Monthly finance details added successfully for customer ID: {}", customerId);
	}

	private void getTransactionMap(Map<String, Object> txMap, Long id, Long accountNumber, Long limit, Long from,
			Long to, Long offset) {
		addToMapIfValid(txMap, "customerId", id);
		addToMapIfValid(txMap, "accountNumber", accountNumber);
		addToMapIfValid(txMap, "limit", limit);
		addToMapIfValid(txMap, "from", from);
		addToMapIfValid(txMap, "to", to);
		addToMapIfValid(txMap, "offset", offset);
	}

	private void addToMapIfValid(Map<String, Object> map, String key, Long value) {
		if (value != null && value > 0) {
			map.put(key, value);
		}
	}
}
