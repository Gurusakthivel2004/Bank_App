package service;

import java.util.Map;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import dblayer.dao.AccountDAO;
import dblayer.model.Account;
import util.CustomException;
import util.Helper;

public class AccountService {

	private final Logger logger = LogManager.getLogger(AccountService.class);
	private AccountDAO accountDAO = new AccountDAO();
	private CacheService cacheService = new CacheService();

	/**
	 * Fetches account details based on input parameters.
	 * 
	 * @param customerId     The ID of the customer.
	 * @param accountNumber  The account number (0 if not used).
	 * @param branchId       The ID of the branch (0 if not used).
	 * @param accountCreated Timestamp representing when the account was created.
	 * @return List of accounts matching the search criteria.
	 * @throws CustomException If an error occurs during account retrieval.
	 */

	@SuppressWarnings({ "unchecked", "unlikely-arg-type" })
	public List<Account> getAccountDetails(Long customerId, Long accountNumber, Long branchId, Long accountCreated)
			throws CustomException {
		logger.info("Fetching account details for customerId: {}, accountNumber: {}, branchId: {}, accountCreated: {}",
				customerId, accountNumber, branchId, accountCreated);
		logger.info(Helper.getThreadLocalValue().get("branchId"));

		try {
			String key = generateCacheKey(customerId, accountNumber, branchId);
			Map<String, Account> cachedAccounts = cacheService.get(key, String.class, Account.class);
			// Return cached accounts if available
			if (cachedAccounts != null && cachedAccounts.containsKey(accountCreated) ) {
				return (List<Account>) cachedAccounts;
			}
			// Fetch accounts from database
			List<Account> accounts = fetchAccounts(customerId, accountNumber, branchId, accountCreated);
			if (accounts != null && !accounts.isEmpty()) {
				// Filter based on branch ID if needed
				List<Account> filteredAccounts = filterAccountsByBranch(accounts);

				// Save to cache if cache key exists
				if (!key.isEmpty()) {
					saveToCache(key, accountCreated, filteredAccounts);
				}
				return filteredAccounts;
			}
			return accounts;
		} catch (Exception e) {
			logger.error("Error fetching account details: {}", e.getMessage());
			throw new CustomException("Unable to fetch account details. Please try again later.", e);
		}
	}

	private String generateCacheKey(Long customerId, Long accountNumber, Long branchId) {
		System.out.println(customerId + " " + branchId);
		if (accountNumber == 0L && branchId == 0L) {
			return "customerIdAccounts:" + customerId;
		} else if (customerId == 0L && accountNumber == 0L) {
			return "branchIdAccounts:" + branchId;
		}
		return "";
	}

	private List<Account> fetchAccounts(Long customerId, Long accountNumber, Long branchId, Long accountCreated) throws CustomException {
		if (accountCreated > 0) {
			return accountDAO.getAccounts(customerId, accountNumber, branchId, accountCreated, 8L);
		}
		return accountDAO.getAccounts(customerId, accountNumber, branchId, accountCreated, 0L);
	}

	private List<Account> filterAccountsByBranch(List<Account> accounts) throws CustomException {
		if (Helper.getThreadLocalValue().get("branchId") != null) {
			logger.debug("Filtering accounts based on branch ID");
			return accountDAO.checkAccountBranchId(accounts);
		}
		return accounts;
	}

	private void saveToCache(String key, Long accountCreated, List<Account> accounts) {
		Map<Long, List<Account>> accountMap = new HashMap<>();
		accountMap.put(accountCreated, accounts);
		cacheService.save(key, accountMap);
		logger.debug("Saved accounts to cache with key '{}'", key);
	}

	/**
	 * Creates a new account using the provided account data.
	 * 
	 * @param accountMap A map containing the account details.
	 * @throws CustomException If the account creation process fails.
	 */

	public void createAccount(Map<String, Object> accountMap) throws CustomException {
		logger.info("Creating a new account with data: {}", accountMap);

		try {
			accountMap.put("minBalance", new BigDecimal(500));
			accountMap.put("branchId", Helper.getThreadLocalValue().get("branchId"));
			accountMap.put("accountNumber", 7018120L);
			accountMap.put("status", "Active");
			accountMap.put("performedBy", Helper.getThreadLocalValue().get("id"));

			long userId = Long.parseLong((String) accountMap.get("userId"));
			boolean isPrimary = getAccountDetails(userId, 0l, 0l, 0l).isEmpty();
			accountMap.put("isPrimary", isPrimary);

			logger.debug("Populated accountMap with additional data: {}", accountMap);

			Account account = Helper.createPojoFromMap(accountMap, Account.class);
			logger.debug("Converted accountMap to Account object: {}", account);

			accountDAO.createAccount(account);
			logger.info("Account successfully created with accountNumber: {}", account.getAccountNumber());
		} catch (CustomException e) {
			logger.error("Error creating account: {}", e.getMessage());
			throw e;
		} catch (Exception e) {
			logger.error("Unexpected error during account creation: {}", e.getMessage());
			throw new CustomException("Account creation failed. Please contact support.", e);
		}
	}

	/**
	 * Deletes an account based on the provided account ID.
	 * 
	 * @param accountId The ID of the account to be deleted.
	 * @throws CustomException If the account cannot be found or deletion fails.
	 */

	public void deleteAccount(Long accountId) throws CustomException {
		logger.info("Attempting to delete account with ID: {}", accountId);

		try {
			List<Account> accounts = accountDAO.getAccounts(accountId, 0l, 0l, 0l, 0l);
			if (accounts.isEmpty()) {
				logger.error("No acc" + "ount found with ID: {}", accountId);
				throw new CustomException("Account not found for ID: " + accountId);
			}

			accountDAO.removeAccount(accountId);
			logger.info("Account successfully deleted with ID: {}", accountId);
		} catch (CustomException e) {
			logger.error("Error deleting account with ID {}: {}", accountId, e.getMessage());
			throw e;
		} catch (Exception e) {
			logger.error("Unexpected error during account deletion: {}", e.getMessage());
			throw new CustomException("Account deletion failed. Please contact support.", e);
		}
	}
}