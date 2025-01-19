package service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

import Enum.Constants.HttpStatusCodes;
import cache.CacheUtil;
import dao.AccountDAO;
import dao.DAO;
import dao.DAOJoin;
import model.Account;
import model.ColumnCriteria;
import model.JoinObject;
import util.CustomException;
import util.Helper;

public class AccountService {

	private final Logger logger = LogManager.getLogger(AccountService.class);

	private final CacheUtil cacheUtil = new CacheUtil();

	private DAO<Account> accountDAO = new AccountDAO();

	public void updateAccount(Long accountNumber, Map<String, Object> accountMap) throws CustomException {
		logger.info("Attempting to update account details.");

		List<String> fields = new ArrayList<>(Arrays.asList("modifiedAt"));
		List<Object> values = new ArrayList<>(Arrays.asList(System.currentTimeMillis()));

		for (String key : accountMap.keySet()) {
			fields.add(key);
			values.add(accountMap.get(key));
			accountMap.remove(key);
		}

		ColumnCriteria columnCriteria = new ColumnCriteria().setFields(fields).setValues(values);
		accountMap.put("accountNumber", accountNumber);

		accountDAO.update(columnCriteria, accountMap);
		cacheUtil.delete("Accounts");
		logger.info("Account successfully updated with account number: {}", accountNumber);

	}

	public Map<String, Object> getAccountDetails(Map<String, Object> accountMap) throws CustomException {
		String key = "accountInfo";
		Map<String, Object> accountsResult = new HashMap<>();
		List<Object> cachedAccounts = cacheUtil.getCachedList(key, new TypeReference<List<Object>>() {
		}, accountMap, "accountNumber");
		if (cachedAccounts != null) {
			accountsResult = new HashMap<>();
			accountsResult.put("count", cachedAccounts.size());
			accountsResult.put("accounts", cachedAccounts);
			return accountsResult;
		}

		addDefaultFilters(accountMap);

		Long offset = (Long) accountMap.getOrDefault("offset", -1L);
		if (offset != -1) {
			handleOffsetBasedFetch(accountMap, accountsResult, offset);
		} else {
			handleAccountDataFetch(accountMap, accountsResult, key);
		}
		return accountsResult;
	}

	private void addDefaultFilters(Map<String, Object> accountMap) {
		Long branchId = Helper.parseLong(accountMap.getOrDefault("branchId", "0"));
		Long customerId = Helper.parseLong(accountMap.getOrDefault("userId", "0"));
		String role = (String) Helper.getThreadLocalValue("role");
		if (!(accountMap.containsKey("accountNumber") || branchId > 0 && role.equals("Manager"))) {
			if (customerId == null || customerId == -1) {
				accountMap.put("userId", (Long) Helper.getThreadLocalValue("id"));
			}
			if (branchId == null || branchId == -1) {
				accountMap.put("branchId", (Long) Helper.getThreadLocalValue("branchId"));
			}
		}
		if ("Employee".equals(role)) {
			accountMap.putIfAbsent("branchId", (Long) Helper.getThreadLocalValue("branchId"));
		}
	}

	private void handleOffsetBasedFetch(Map<String, Object> accountMap, Map<String, Object> accountsResult, Long offset)
			throws CustomException {
		if (offset == 0) {
			Long count = accountDAO.getDataCount(accountMap);
			accountsResult.put("count", count);
		}
		DAOJoin<Account> accountDaoJoin = new AccountDAO();
		List<JoinObject<Account>> joinModels = accountDaoJoin.getJoined(accountMap);
		accountsResult.put("accounts", joinModels);
	}

	private void handleAccountDataFetch(Map<String, Object> accountMap, Map<String, Object> accountsResult, String key)
			throws CustomException {
		List<Account> accounts = accountDAO.get(accountMap);
		accountsResult.put("accounts", accounts);
		if (accountMap.containsKey("accountNumber")) {
			cacheUtil.save(key + accountMap.get("accountNumber"), accounts);
		}
	}

	@SuppressWarnings("unchecked")
	public void createAccount(Map<String, Object> accountMap) throws CustomException {

		Map<String, Object> accountFetchMap = new HashMap<>();
		accountFetchMap.put("userId", Helper.parseLong(accountMap.get("userId")));
		List<Account> accounts = (List<Account>) getAccountDetails(accountFetchMap).get("accounts");

		accountMap.put("minBalance", new BigDecimal(500));
		accountMap.put("branchId", Helper.getThreadLocalValue("branchId"));
		accountMap.put("accountNumber", 7018120L);
		accountMap.put("status", "Active");
		accountMap.put("performedBy", Helper.getThreadLocalValue("id"));

		if (accountMap.get("accountType").equals("Operational") && accounts.size() > 0) {
			throw new CustomException("Operational account already exists for the id", HttpStatusCodes.BAD_REQUEST);
		}
		accountMap.put("isPrimary", accounts.isEmpty());

		Account account = Helper.createPojoFromMap(accountMap, Account.class);

		accountDAO.create(account);
		logger.info("Account successfully created with accountNumber: {}", account.getAccountNumber());

	}

}