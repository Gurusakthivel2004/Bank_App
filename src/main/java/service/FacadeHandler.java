package service;

import java.math.BigDecimal;

import java.time.LocalDate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import Enum.Constants.HttpStatusCodes;
import model.Account;
import model.Transaction;

import util.CustomException;
import util.Helper;

public class FacadeHandler {

	private final Logger logger = LogManager.getLogger(FacadeHandler.class);
	private TransactionService transactionService = new TransactionService();
	private UserService userService = new UserService();
	private AccountService accountService = new AccountService();

	@SuppressWarnings("unchecked")
	public Map<String, Object> dashBoardDetails() throws CustomException {
		long id = (Long) Helper.getThreadLocalValue().get("id");
		Map<String, Object> map = new HashMap<>();

		logger.info("Fetching dashboard details for user ID: {}", id);

		try {
			logger.debug("Fetching account details for user ID: {}", id);
			Map<String, Object> accountMap = new HashMap<>();
			accountMap.put("userId", id);
			List<Account> accounts = (List<Account>) accountService.getAccountDetails(accountMap).get("accounts");
			map.put("account", accounts);
			logger.info(accounts);

			logger.info("Fetching transaction details for user ID: {}", id);
			// Fetch transaction details
			List<Transaction> transactions = new ArrayList<>();
			for (int i = 0; i < accounts.size(); i++) {
				Account account = accounts.get(i);
				Map<String, Object> txMap = new HashMap<>();
				getTransactionMap(txMap, id, account.getAccountNumber(), 5l, 0l, 0l, -1l);
				List<Transaction> accountTransactions = (List<Transaction>) transactionService
						.getTransactionDetails(txMap).get("transactions");
				transactions.addAll(accountTransactions);
			}

			logger.info(transactions);
			map.put("transactions", transactions);

			// Fetch user details based on role
			String role = (String) Helper.getThreadLocalValue().get("role");
			Map<String, Object> userMap = new HashMap<>();
			userMap.put("role", role);
			userMap.put("userId", id);
			map.put("userDetail", userService.getUserDetails(userMap, false).get("userDetail"));

			// Fetch branch details for the accounts
			logger.debug("Fetching branch details for user ID: {}", id);
			List<Object> branchDetails = new ArrayList<>();
			BranchService branchService = new BranchService();
			for (Account account : accounts) {
				Object branch = branchService.getBranchDetails(account.getBranchId(), false).get(0);
				if (!branchDetails.contains(branch)) {
					branchDetails.add(branch);
				}
			}
			map.put("branch", branchDetails);

			logger.debug("Adding monthly finance details for user ID: {}", id);
			addMonthlyFinance(map, id, 3);

			logger.info("Dashboard details fetched successfully for user ID: {}", id);
		} catch (CustomException e) {
			logger.error("Error occurred while fetching dashboard details for user ID: {}: {}", id, e);
			throw e;
		} catch (Exception e) {
			logger.error("Unexpected error occurred while fetching dashboard details for user ID: {}: {}", id,
					e.getMessage(), e);
			throw new CustomException("An unexpected error occurred while fetching details.",
					HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}

		return map;
	}

	@SuppressWarnings("unchecked")
	private void addMonthlyFinance(Map<String, Object> map, Long customerId, int monthLength) throws CustomException {
		int currentMonth = LocalDate.now().getMonthValue();
		int currentYear = LocalDate.now().getYear();
		logger.debug("Adding monthly finance details for customer ID: {}", customerId);

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

		if (id != null && id > 0) {
			txMap.put("customerId", id);
		}
		if (accountNumber != null && accountNumber > 0) {
			txMap.put("accountNumber", accountNumber);
		}
		if (limit != null && limit > 0) {
			txMap.put("limit", limit);
		}
		if (from != null && from > 0) {
			txMap.put("from", from);
		}
		if (to != null && to > 0) {
			txMap.put("to", to);
		}
		if (offset != null && offset > 0) {
			txMap.put("offset", offset);
		}
	}

}
