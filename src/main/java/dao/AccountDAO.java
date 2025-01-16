package dao;

import java.math.BigInteger;

import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import Enum.Constants.HttpStatusCodes;

import model.Account;
import model.Criteria;
import model.JoinObject;
import model.ColumnCriteria;

import util.CustomException;
import util.SQLHelper;

public class AccountDAO {

	private static final Logger logger = LogManager.getLogger(AccountDAO.class);

	public <T> void updateAccount(ColumnCriteria columnCriteria, String column, Object value) throws CustomException {
		try {
			Criteria criteria = new Criteria().setClazz(Account.class);
			DAOHelper.addCondition(criteria, value != null, column, "EQUAL_TO", value);

			SQLHelper.update(columnCriteria, criteria);
		} catch (SQLException e) {
			logger.error("Error updating account.", e);
			throw new CustomException("Failed to update account", HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

	public List<Account> getAccounts(Map<String, Object> accountMap) throws CustomException {
		Criteria criteria = DAOHelper.getAccountCriteria(accountMap);
		try {
			return SQLHelper.get(criteria, Account.class);
		} catch (SQLException e) {
			logger.error("Error while fetching account details: ", e);
			throw new CustomException("Failed to fetch account details: ", HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

	public Long getDataCount(Map<String, Object> accountMap) throws CustomException {
		try {
			Criteria criteria = DAOHelper.getAccountCriteria(accountMap);
			criteria.setOffsetValue(-1L).setAggregateFunction("COUNT").setAggregateOperator("*");
			Long count = SQLHelper.getCount(criteria, Account.class);
			if (count == 0) {
				throw new CustomException("Unexpected error occured while fetching account details",
						HttpStatusCodes.INTERNAL_SERVER_ERROR);
			}
			return count;
		} catch (SQLException e) {
			logger.error("Error while fetching account details: ", e);
			throw new CustomException("Failed to fetch account details: ", HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

	public List<JoinObject<Account>> getJoinedAccounts(Map<String, Object> accountMap) throws CustomException {
		Criteria branchJoinCriteria = DAOHelper.buildJoinCriteria(Account.class, Arrays.asList("branch"),
				new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
				new ArrayList<>(), " JOIN ", true);
		branchJoinCriteria.setSelectColumn(Arrays.asList("account.*", "branch.name"));

		DAOHelper.addJoinCondition(branchJoinCriteria, true, "account.branch_id", "EQUAL_TO", "branch.id");
		DAOHelper.applyAccountFilters(branchJoinCriteria, accountMap);

		try {
			return SQLHelper.getJoinedObjects(branchJoinCriteria, Account.class);
		} catch (SQLException e) {
			logger.error("Error while fetching account details: ", e);
			throw new CustomException("Failed to fetch account details: ", HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

	public void create(Account account) throws CustomException {
		logger.info("Creating account: {}", account);
		try {
			account.setCreatedAt(System.currentTimeMillis());

			Long accountId = ((BigInteger) SQLHelper.insert(account)).longValue();
			logger.debug("Account ID generated: {}", accountId);

			ColumnCriteria columnCriteria = new ColumnCriteria().setFields(Arrays.asList("accountNumber"));
			String accountNumber = "701" + String.format("%04d", account.getBranchId())
					+ String.format("%04d", accountId);
			columnCriteria.setValues(Arrays.asList(Long.parseLong(accountNumber)));

			Criteria criteria = new Criteria().setClazz(Account.class);
			criteria.getColumn().add("account_id");
			criteria.getOperator().add("EQUAL_TO");
			criteria.getValue().add(accountId);

			updateAccount(columnCriteria, "account_id", accountId);
		} catch (Exception e) {
			logger.error("Error creating account: {}", account, e);
			throw new CustomException("Failed to create account", HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

}
