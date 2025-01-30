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

import Enum.Constants.AccountType;
import Enum.Constants.HttpStatusCodes;
import Enum.Constants.LogType;
import Enum.Constants.Role;
import Enum.Constants.Status;
import cache.CacheUtil;
import dao.AccountDAO;
import dao.DAO;
import dao.DAOJoin;
import model.Account;
import model.ActivityLog;
import model.ColumnCriteria;
import model.JoinObject;
import util.CustomException;
import util.Helper;
import util.ValidationUtil;

public class AccountService {

	private final Logger logger = LogManager.getLogger(AccountService.class);

	private final CacheUtil cacheUtil = new CacheUtil();

	private DAO<Account> accountDAO = new AccountDAO();

	private final AuthorizationService authService = new AuthorizationService();

	public void updateAccount(Long accountNumber, Map<String, Object> accountMap) throws CustomException {
		logger.info("Attempting to update account details.");

		if (accountMap == null || accountMap.size() == 0) {
			throw new CustomException("Please enter fields to update", HttpStatusCodes.BAD_REQUEST);
		}

		ValidationUtil.validateUpdateFields(accountMap, Account.class);

		List<String> fields = new ArrayList<>(Arrays.asList("modifiedAt", "performedBy"));
		List<Object> values = new ArrayList<>(
				Arrays.asList(System.currentTimeMillis(), Helper.getThreadLocalValue("id")));

		StringBuilder logMessage = new StringBuilder("updated fields: ");
		StringBuilder logValues = new StringBuilder("with values: ");

		for (Map.Entry<String, Object> entry : accountMap.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			if ("modifiedAt".equals(key) || "performedBy".equals(key)) {
				continue;
			}

			fields.add(key);
			values.add(value);

			logMessage.append(key).append(", ");
			logValues.append(value).append(", ");
		}

		if (logMessage.length() > 0) {
			logMessage.setLength(logMessage.length() - 2);
			logValues.setLength(logValues.length() - 2);
		}

		ColumnCriteria columnCriteria = new ColumnCriteria().setFields(fields).setValues(values);
		accountMap = new HashMap<>();
		accountMap.put("accountNumber", accountNumber);

		// Account validation check.
		List<Account> accounts = accountDAO.get(accountMap);
		if (accounts == null || accounts.isEmpty()) {
			throw new CustomException("Account not found.", HttpStatusCodes.BAD_REQUEST);
		}
		Role role = Role.fromString((String) Helper.getThreadLocalValue("role"));
		if (role == Role.Employee) {
			if (accounts.get(0).getUserId() != Helper.getThreadLocalValue("")) {
				throw new CustomException("Not authorized to update the account", HttpStatusCodes.BAD_REQUEST);
			}
		}
		accountDAO.update(columnCriteria, accountMap);

		ActivityLog activityLog = new ActivityLog().setLogMessage(logMessage.toString() + " " + logValues.toString())
				.setLogType(LogType.Update).setUserAccountNumber(accountNumber).setRowId(accountNumber)
				.setTableName("Account").setUserId(null);

		TaskExecutorService.getInstance().submit(activityLog);

		cacheUtil.delete("accountInfo");
		logger.info("Account successfully updated with account number: {}", accountNumber);
	}

	public Map<String, Object> getAccountDetails(Map<String, Object> accountMap) throws CustomException {
		String key = "accountInfo";
		Map<String, Object> accountsResult = new HashMap<>();
		List<Account> cachedAccounts = cacheUtil.getCachedList(key, new TypeReference<List<Account>>() {
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

	private void addDefaultFilters(Map<String, Object> accountMap) throws CustomException {
		Long branchId = Helper.parseLong(accountMap.getOrDefault("branchId", "0"));
		Long customerId = Helper.parseLong(accountMap.getOrDefault("userId", "0"));
		Role role = Role.fromString((String) Helper.getThreadLocalValue("role"));
		if (!(accountMap.containsKey("accountNumber") || branchId > 0 && role == Role.Manager)) {
			if (customerId == null || customerId == -1) {
				accountMap.put("userId", (Long) Helper.getThreadLocalValue("id"));
			}
			if (branchId == null || branchId == -1) {
				accountMap.put("branchId", (Long) Helper.getThreadLocalValue("branchId"));
			}
		}
		if (role == Role.Employee) {
			accountMap.put("branchId", (Long) Helper.getThreadLocalValue("branchId"));
		}
		if (!accountMap.containsKey("status")) {
			accountMap.put("status", Status.Active);
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
		List<Account> accounts = new ArrayList<>();

		for (JoinObject<Account> joinModel : joinModels) {
			Account account = (Account) joinModel.getInstance();
			accounts.add(account);
		}
		if (!authService.isAuthorized("account", accounts)) {
			throw new CustomException("Not authorized to access account details", HttpStatusCodes.UNAUTHORIZED);
		}
		accountsResult.put("accounts", joinModels);
	}

	private void handleAccountDataFetch(Map<String, Object> accountMap, Map<String, Object> accountsResult, String key)
			throws CustomException {
		List<Account> accounts = accountDAO.get(accountMap);

		if (!authService.isAuthorized("account", accounts)) {
			throw new CustomException("Not authorized to access account details", HttpStatusCodes.UNAUTHORIZED);
		}

		accountsResult.put("accounts", accounts);
		if (accountMap.containsKey("accountNumber")) {
			cacheUtil.save(key + accountMap.get("accountNumber"), accounts);
		}
	}

	@SuppressWarnings("unchecked")
	public void createAccount(Map<String, Object> accountMap) throws CustomException {
		logger.info("Creating account..");

		ValidationUtil.validateCreateAccount(accountMap);

		Map<String, Object> accountFetchMap = new HashMap<>();
		accountFetchMap.put("userId", Helper.parseLong(accountMap.get("userId")));
		List<Account> accounts = (List<Account>) getAccountDetails(accountFetchMap).get("accounts");

		logger.info(accounts);

		accountMap.put("minBalance", new BigDecimal(500));
		accountMap.put("branchId", Helper.getThreadLocalValue("branchId"));
		accountMap.put("accountNumber", 7018120L);
		accountMap.put("status", Status.Active);
		accountMap.put("performedBy", Helper.getThreadLocalValue("id"));

		AccountType accountType = AccountType.fromString((String) accountMap.get("accountType"));
		if (accountType == AccountType.Operational && accounts.size() > 0) {
			throw new CustomException("Operational account already exists for the id", HttpStatusCodes.BAD_REQUEST);
		}
		accountMap.put("accountType", accountType);
		accountMap.put("isPrimary", accounts.isEmpty());

		Account account = Helper.createPojoFromMap(accountMap, Account.class);

		Long accountId = accountDAO.create(account);
		logger.info("Account successfully created with accountId: {}", accountId);

		ActivityLog activityLog = new ActivityLog().setLogMessage("Account created").setLogType(LogType.Insert)
				.setRowId(accountId).setTableName("Account").setUserId(Long.getLong((String) accountMap.get("userId")));

		TaskExecutorService.getInstance().submit(activityLog);

	}

}