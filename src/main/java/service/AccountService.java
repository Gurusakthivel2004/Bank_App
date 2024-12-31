package service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.core.type.TypeReference;
import dblayer.dao.AccountDAO;
import dblayer.model.Account;
import dblayer.model.ColumnCriteria;
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

	@SuppressWarnings("unchecked")
	public Map<String, Object> getAccountDetails(Map<String, Object> accountMap) throws CustomException {

		try {
			Long customerId = (Long) accountMap.getOrDefault("userId", 0l), branchId = (Long) accountMap.getOrDefault("branchId", 0l);
			if (!(accountMap.containsKey("accountNumber") || branchId > 0)) {
				if (customerId == null || customerId == -1) {
					accountMap.put("userId", (Long) Helper.getThreadLocalValue().get("id"));
				}
				if (branchId == null || branchId == -1) {
					accountMap.put("branchId", (Long) Helper.getThreadLocalValue().get("branchId"));
				}
			}
			String key = generateCacheKey(accountMap);
			Map<Long, List<Account>> cachedAccounts = cacheService.get(key,
					new TypeReference<Map<Long, List<Account>>>() {
					});
			// Return cached accounts if available
			Long cachedKey = extractLongFromString(key);
			if (cachedAccounts != null && cachedAccounts.containsKey(cachedKey)) {
				return (Map<String, Object>) cachedAccounts.get(cachedKey);
			}
			// Fetch accounts from database
			Map<String, Object> accountsResult = fetchAccounts(accountMap);
			List<Account> accounts = (List<Account>) accountsResult.get("accounts");
			if (accounts != null && !accounts.isEmpty()) {
				// Filter based on branch ID if needed
				List<Account> filteredAccounts = filterAccountsByBranch(accounts);
				if (!key.isEmpty()) {
					saveToCache(key, cachedKey, accountsResult);
				}
				accountsResult.put("accounts", filteredAccounts);
			}
			return accountsResult;
		} catch (Exception e) {
			logger.error("Error fetching account details: {}", e);
			throw new CustomException("Unable to fetch account details. Please try again later.", e);
		}
	}

	private String generateCacheKey(Map<String, Object> accountMap) {
		if (accountMap == null) {
			return "";
		}
		Long userId = (Long) accountMap.getOrDefault("userId", 0L);
		Long accountNumber = (Long) accountMap.getOrDefault("accountNumber", 0L);
		Long branchId = (Long) accountMap.getOrDefault("branchId", 0L);
		if (accountNumber == 0L && branchId == 0L) {
			return "customerIdAccounts:" + userId;
		} else if (userId == 0L && accountNumber == 0L) {
			return "branchIdAccounts:" + branchId;
		}
		return "";
	}

	private Long extractLongFromString(String str) {
		if (str == null || str.isEmpty()) {
			return null;
		}
		Pattern pattern = Pattern.compile("\\d+");
		Matcher matcher = pattern.matcher(str);
		if (matcher.find()) {
			try {
				return Long.parseLong(matcher.group());
			} catch (NumberFormatException e) {
				logger.error("Number exceeds Long range.", e);
				return null;
			}
		}
		return null;
	}

	private Map<String, Object> fetchAccounts(Map<String, Object> accountMap) throws CustomException {
		return accountDAO.getAccounts(accountMap);
	}

	private List<Account> filterAccountsByBranch(List<Account> accounts) throws CustomException {
		if (Helper.getThreadLocalValue().get("branchId") != null) {
			logger.debug("Filtering accounts based on branch ID");
			return checkAccountBranchId(accounts);
		}
		return accounts;
	}

	public List<Account> checkAccountBranchId(List<Account> accounts) throws CustomException {
		logger.info("Checking accounts against branchId in ThreadLocal.");
		try {
			long branchId = (Long) Helper.getThreadLocalValue().get("branchId");
			logger.debug("Branch ID retrieved from ThreadLocal: {}", branchId);

			List<Account> filteredAccounts = accounts.stream().filter(account -> account.getBranchId() == branchId)
					.collect(Collectors.toList());

			logger.info("{} accounts matched the branchId: {}", filteredAccounts.size(), branchId);
			return filteredAccounts;
		} catch (Exception e) {
			logger.error("Error checking accounts against branchId.", e);
			throw new CustomException("Failed to check accounts");
		}
	}

	private void saveToCache(String key, Long accountCreated, Map<String, Object> accountMap) {
		cacheService.save(key, accountMap);
		logger.debug("Saved accounts to cache with key '{}'", key);
	}

	/**
	 * Creates a new account using the provided account data.
	 * 
	 * @param accountMap A map containing the account details.
	 * @throws CustomException If the account creation process fails.
	 */

	@SuppressWarnings("unchecked")
	public void createAccount(Map<String, Object> accountMap) throws CustomException {
		logger.info("Creating a new account with data: {}", accountMap);
		try {
			accountMap.put("minBalance", new BigDecimal(500));
			accountMap.put("branchId", Helper.getThreadLocalValue().get("branchId"));
			accountMap.put("accountNumber", 7018120L);
			accountMap.put("status", "Active");
			accountMap.put("performedBy", Helper.getThreadLocalValue().get("id"));

			List<Account> accounts = (List<Account>) getAccountDetails(accountMap);
			boolean isPrimary = accounts.isEmpty();
			accountMap.put("isPrimary", isPrimary);
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

	public void updateAccount(Long accountNumber, String key, Object value) throws CustomException {
		logger.info("Attempting to update account details.");
		try {
			ColumnCriteria columnCriteria = new ColumnCriteria();
			columnCriteria.setFields(Arrays.asList(key, "modifiedAt"));
			columnCriteria.setValues(Arrays.asList(value, System.currentTimeMillis()));

			accountDAO.updateAccount(columnCriteria, "account_number", accountNumber);
			cacheService.delete("Accounts");
			logger.info("Account successfully updated with account number: {}", accountNumber);
		} catch (CustomException e) {
			logger.error("Error updating account. Error: {}", e.getMessage());
			throw e;
		}
	}

	/**
	 * Deletes an account based on the provided account ID.
	 * 
	 * @param accountId The ID of the account to be deleted.
	 * @throws CustomException If the account cannot be found or deletion fails.
	 */

	public void deleteAccount(Map<String, Object> accountMap) throws CustomException {
		logger.info("Attempting to delete account");

		try {
			Map<String, Object> accounts = accountDAO.getAccounts(accountMap);
			if (accounts.isEmpty()) {
				throw new CustomException("Account not found");
			}
			accountDAO.removeAccount(accountMap);
			logger.info("Account successfully deleted");
		} catch (CustomException e) {
			logger.error("Error deleting account", e.getMessage());
			throw e;
		} catch (Exception e) {
			logger.error("Unexpected error during account deletion: {}", e.getMessage());
			throw new CustomException("Account deletion failed. Please contact support.", e);
		}
	}
}