package dblayer.dao;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import dblayer.model.Account;
import dblayer.model.ColumnCriteria;
import dblayer.model.Criteria;
import util.CustomException;
import util.Helper;
import util.SQLHelper;

public class AccountDAO {

	private static final Logger logger = LogManager.getLogger(AccountDAO.class);

	public void createAccount(Account account) throws CustomException {
		logger.info("Creating account: {}", account);
		try {
			account.setCreatedAt(System.currentTimeMillis());
			logger.debug("Account creation timestamp set: {}", account.getCreatedAt());

			Long accountId = ((BigInteger) SQLHelper.insert(account)).longValue();
			logger.debug("Account ID generated: {}", accountId);

			ColumnCriteria columnCriteria = new ColumnCriteria();
			columnCriteria.setFields(Arrays.asList("accountNumber"));
			String accountNumber = "701" + String.format("%04d", account.getBranchId())
					+ String.format("%04d", accountId);
			columnCriteria.setValues(Arrays.asList(Long.parseLong(accountNumber)));

			logger.debug("Account number generated: {}", accountNumber);

			Criteria criteria = new Criteria();
			criteria.setClazz(Account.class);
			criteria.getColumn().add("account_id");
			criteria.getOperator().add("=");
			criteria.getValue().add(accountId);

			updateAccount(columnCriteria, "account_id", accountId);
			logger.info("Account created successfully: {}", account);
		} catch (Exception e) {
			logger.error("Error creating account: {}", account, e);
			throw new CustomException("Failed to create account");
		}
	}

	public Map<String, Object> getAccounts(Map<String, Object> accountMap) throws CustomException {
		logger.info("Fetching accounts with parameters: {}", accountMap);
		try {
			Criteria criteria = new Criteria();
			criteria.setClazz(Account.class);
			criteria.setSelectColumn(new ArrayList<>(Arrays.asList("*")));

			Long userId = (Long) accountMap.getOrDefault("userId", 0L);
			Long accountNumber = (Long) accountMap.getOrDefault("accountNumber", 0L);
			Long branchId = (Long) accountMap.getOrDefault("branchId", 0L);
			Long accountCreated = (Long) accountMap.getOrDefault("accountCreated", 0L);
			Long limitValue = (Long) accountMap.getOrDefault("limit", 0L);
			Long offset = (Long) accountMap.getOrDefault("offset", -1L);
			String type = (String) accountMap.getOrDefault("accountType", "");
			String status = (String) accountMap.getOrDefault("status", "");

			Helper.addCondition(criteria, userId > 0, "user_id", "=", userId);
			Helper.addCondition(criteria, accountNumber > 0, "account_number", "LIKE", "%" + accountNumber + "%");
			Helper.addCondition(criteria, accountCreated > 0, "created_at", "=", accountCreated);
			Helper.addCondition(criteria, branchId > 0, "branch_id", "=", branchId);
			Helper.addCondition(criteria, type != "", "account_type", "=", type);
			Helper.addCondition(criteria, status != "", "status", "=", status);

			if (limitValue > 0) {
				criteria.setLimitValue(limitValue);
			}
			if (criteria.getColumn().size() > 1) {
				criteria.setLogicalOperator("AND");
			}

			Map<String, Object> txResult = new HashMap<>();
			if (offset >= 0) {
				if (offset == 0) {
					criteria.setOffsetValue(-1l);
					txResult.put("count", SQLHelper.get(criteria).get(0));
				}
				criteria.setOffsetValue(offset);
			}

			logger.debug("Criteria for fetching accounts: {}", criteria);
			txResult.put("accounts", SQLHelper.get(criteria));
			return txResult;
		} catch (Exception e) {
			logger.error("Error fetching accounts.", e);
			throw new CustomException("Failed to fetch accounts");
		}
	}

	public <T> void updateAccount(ColumnCriteria columnCriteria, String column, Object value) throws CustomException {
		logger.info("Updating account with ColumnCriteria: {}", columnCriteria);
		try {
			Helper.checkNullValues(columnCriteria);
			Helper.checkNullValues(value);
			Helper.checkNullValues(column);
			Criteria criteria = new Criteria();
			criteria.setClazz(Account.class);
			Helper.addCondition(criteria, value != null, column, "=", value);

			SQLHelper.update(columnCriteria, criteria);
			logger.info("Account updated successfully.");
		} catch (Exception e) {
			logger.error("Error updating account.", e);
			throw new CustomException("Failed to update account");
		}
	}

	public void removeAccount(Map<String, Object> accountMap) throws CustomException {
		Long accountId = (Long) accountMap.get("accountId");
		logger.info("Removing account with accoutnId: {}", accountId);
		try {
			ColumnCriteria columnCriteria = new ColumnCriteria();
			columnCriteria.setFields(Arrays.asList("status"));
			columnCriteria.setValues(Arrays.asList("Suspended"));

			Criteria criteria = new Criteria();
			criteria.setClazz(Account.class);

			Helper.addCondition(criteria, accountId > 0, "account_id", "=", accountId);

			logger.debug("Criteria for removing account: {}", criteria);
			updateAccount(columnCriteria, "account_id", accountId);

			logger.info("Account suspended successfully.");
		} catch (Exception e) {
			logger.error("Error removing account.", e);
			throw new CustomException("Failed to remove account.");
		}
	}
}
