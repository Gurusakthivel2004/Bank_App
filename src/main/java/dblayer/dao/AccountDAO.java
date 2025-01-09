package dblayer.dao;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

	public <T> void updateAccount(ColumnCriteria columnCriteria, String column, Object value) throws CustomException {
		logger.info("Updating account with ColumnCriteria: {}", columnCriteria);
		try {
			Criteria criteria = new Criteria();
			criteria.setClazz(Account.class);
			Helper.addCondition(criteria, value != null, column, "EQUAL_TO", value);

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

			Helper.addCondition(criteria, accountId > 0, "account_id", "EQUAL_TO", accountId);

			logger.debug("Criteria for removing account: {}", criteria);
			updateAccount(columnCriteria, "account_id", accountId);

			logger.info("Account suspended successfully.");
		} catch (Exception e) {
			logger.error("Error removing account.", e);
			throw new CustomException("Failed to remove account.");
		}
	}

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
			criteria.getOperator().add("EQUAL_TO");
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
			Criteria criteria = Helper.initializeCriteria(Account.class);
			criteria = applyBranchFilter(criteria, accountMap);
			applyAccountFilters(criteria, accountMap);
			applyPagination(criteria, accountMap);

			Map<String, Object> result = new HashMap<>();

			Long offset = (Long) accountMap.getOrDefault("offset", -1L);
			if (offset == 0) {
				criteria.setOffsetValue(-1L);
				criteria.setAggregateFunction("COUNT");
				criteria.setAggregateOperator("*");
				result.put("count", SQLHelper.get(criteria).get(0));
				criteria.setOffsetValue(offset);
				result.put("joinedAccounts", fetchJoinedAccounts(accountMap));
			} else {
				result.put("accounts", SQLHelper.get(criteria));
			}
			return result;
		} catch (Exception e) {
			logger.error("Error fetching accounts.", e);
			throw new CustomException("Failed to fetch accounts");
		}
	}

	private void applyAccountFilters(Criteria criteria, Map<String, Object> accountMap) {
		Helper.addConditionIfPresent(criteria, accountMap, "userId", "user_id", "EQUAL_TO", 0L);
		Helper.addConditionIfPresent(criteria, accountMap, "branchId", "branch_id", "EQUAL_TO", 0L);
		Helper.addConditionIfPresent(criteria, accountMap, "accountCreated", "created_at", "EQUAL_TO", 0L);
		Helper.addCondition(criteria, accountMap.get("accountType") != null, "account_type", "EQUAL_TO",
				accountMap.get("accountType"));
		Helper.addCondition(criteria, accountMap.get("status") != null, "status", "EQUAL_TO", accountMap.get("status"));
		Helper.applyAccountNumberFilter(criteria, accountMap);
	}

	private Criteria applyBranchFilter(Criteria criteria, Map<String, Object> accountMap) {
		if (!accountMap.containsKey("branchId")) {
			return criteria;
		}
		criteria = Helper.buildJoinCriteria(Account.class, Arrays.asList("branch"), new ArrayList<>(),
				new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), " JOIN ",
				true);
		criteria.setSelectColumn(Collections.singletonList("account.*"));
		Helper.addJoinCondition(criteria, true, "account.branch_id", "EQUAL_TO", "branch.id");
		return criteria;
	}

	private void applyPagination(Criteria criteria, Map<String, Object> accountMap) {
		Long limit = (Long) accountMap.getOrDefault("limit", 0L);
		Long offset = (Long) accountMap.getOrDefault("offset", -1L);
		if (limit > 0) {
			criteria.setLimitValue(limit);
		}
		if (offset >= 0) {
			criteria.setOffsetValue(offset == 0 ? -1L : offset);
		}
	}

	private List<Object> fetchJoinedAccounts(Map<String, Object> accountMap) throws CustomException {
		Criteria staffJoinCriteria = Helper.buildJoinCriteria(Account.class, Arrays.asList("branch"), new ArrayList<>(),
				new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), " JOIN ",
				true);
		staffJoinCriteria.setSelectColumn(Arrays.asList("account.*", "branch.name"));

		Helper.addJoinCondition(staffJoinCriteria, true, "account.branch_id", "EQUAL_TO", "branch.id");
		applyAccountFilters(staffJoinCriteria, accountMap);

		return SQLHelper.get(staffJoinCriteria);
	}

}
