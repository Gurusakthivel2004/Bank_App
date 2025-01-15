package service;

import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import java.math.BigDecimal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import Enum.Constants.HttpStatusCodes;
import dao.AccountDAO;

import model.Account;
import model.ColumnCriteria;

import util.CustomException;
import util.Helper;

public class AccountService {

	private final Logger logger = LogManager.getLogger(AccountService.class);
	private AccountDAO accountDAO = new AccountDAO();
	private CacheService cacheService = new CacheService();

	@SuppressWarnings("unchecked")
	public void createAccount(Map<String, Object> accountMap) throws CustomException {
		logger.info("Creating a new account with data: {}", accountMap);

		Map<String, Object> accountFetchMap = new HashMap<>();
		accountFetchMap.put("userId", Helper.parseLong(accountMap.get("userId")));
		List<Account> accounts = (List<Account>) getAccountDetails(accountFetchMap).get("accounts");

		accountMap.put("minBalance", new BigDecimal(500));
		accountMap.put("branchId", Helper.getThreadLocalValue().get("branchId"));
		accountMap.put("accountNumber", 7018120L);
		accountMap.put("status", "Active");
		accountMap.put("performedBy", Helper.getThreadLocalValue().get("id"));

		if (accountMap.get("accountType").equals("Operational") && accounts.size() > 0) {
			throw new CustomException("Operational account already exists for the id", HttpStatusCodes.BAD_REQUEST);
		}
		accountMap.put("isPrimary", accounts.isEmpty());
		logger.debug("Populated accountMap with additional data: {}", accountMap);

		Account account = Helper.createPojoFromMap(accountMap, Account.class);
		logger.debug("Converted accountMap to Account object: {}", account);

		accountDAO.createAccount(account);
		logger.info("Account successfully created with accountNumber: {}", account.getAccountNumber());

	}

	public void updateAccount(Long accountNumber, String key, Object value) throws CustomException {
		logger.info("Attempting to update account details.");

		ColumnCriteria columnCriteria = new ColumnCriteria().setFields(Arrays.asList(key, "modifiedAt"))
				.setValues(Arrays.asList(value, System.currentTimeMillis()));

		accountDAO.updateAccount(columnCriteria, "account_number", accountNumber);
		cacheService.delete("Accounts");
		logger.info("Account successfully updated with account number: {}", accountNumber);

	}

	public void deleteAccount(Map<String, Object> accountMap) throws CustomException {
		logger.info("Attempting to delete account");

		Map<String, Object> accounts = accountDAO.getAccounts(accountMap);
		if (accounts.isEmpty()) {
			throw new CustomException("Account not found", HttpStatusCodes.NOT_FOUND);
		}
		accountDAO.removeAccount(accountMap);
		logger.info("Account successfully deleted");

	}

	public Map<String, Object> getAccountDetails(Map<String, Object> accountMap) throws CustomException {
		Long customerId = Helper.parseLong(accountMap.getOrDefault("userId", "0"));
		Long branchId = Helper.parseLong(accountMap.getOrDefault("branchId", "0"));
		String role = (String) Helper.getThreadLocalValue().get("role");

		if (!(accountMap.containsKey("accountNumber") || branchId > 0 && role.equals("Manager"))) {
			if (customerId == null || customerId == -1) {
				accountMap.put("userId", (Long) Helper.getThreadLocalValue().get("id"));
			}
			if (branchId == null || branchId == -1) {
				accountMap.put("branchId", (Long) Helper.getThreadLocalValue().get("branchId"));
			}
		}

		if (role.equals("Employee") && !accountMap.containsKey("branchId")) {
			accountMap.put("branchId", (Long) Helper.getThreadLocalValue().get("branchId"));
		}
		Map<String, Object> accountsResult = accountDAO.getAccounts(accountMap);

		return accountsResult;
	}

}