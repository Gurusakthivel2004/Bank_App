package dao;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import Enum.Constants.HttpStatusCodes;
import model.Account;
import model.ColumnCriteria;
import model.Criteria;
import model.Transaction;
import util.CustomException;
import util.SQLHelper;

public class TransactionDAO {

	private final Logger logger = LogManager.getLogger(TransactionDAO.class);

	public void updateAccountBalance(Long accountNumber, BigDecimal balance) throws CustomException {
		logger.info("Updating account balance for account: " + accountNumber);
		ColumnCriteria columnCriteria = new ColumnCriteria().setFields(Arrays.asList("balance"))
				.setValues(Arrays.asList(balance));

		Criteria criteria = new Criteria().setClazz(Account.class);
		criteria.getColumn().add("account_number");
		criteria.getOperator().add("EQUAL_TO");
		criteria.getValue().add(accountNumber);

		try {
			SQLHelper.update(columnCriteria, criteria);
		} catch (SQLException e) {
			logger.error("Error while updating Account balance: ", e);
			throw new CustomException("Failed to update Account balance: ", HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
		logger.info("Account balance updated successfully.");

	}

	public Long createTransaction(Transaction transaction) throws CustomException {
		Long txId;
		try {
			txId = ((BigInteger) SQLHelper.insert(transaction)).longValue();
		} catch (Exception e) {
			logger.error("Error during inserting data: ", e);
			throw new CustomException("Error during creating transaction: ", HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
		return txId;
	}

	public void removeFailedTransaction(Criteria criteria) throws CustomException {
		try {
			SQLHelper.delete(criteria);
		} catch (SQLException e) {
			logger.error("Error while removing failed transaction: ", e);
			throw new CustomException("Failed to remove failed transaction: ", HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

	public Map<String, Object> getTransactions(Map<String, Object> txMap) throws CustomException {
		logger.info("Fetching transactions with provided filters...");
		Criteria criteria = initializeCriteria();
		criteria = applyBranchFilter(criteria, txMap);
		applyTransactionFilters(criteria, txMap);
		DAOHelper.applyAccountNumberFilter(criteria, txMap);
		applyPagination(criteria, txMap);
		return executeQuery(criteria, txMap);
	}

	private Criteria initializeCriteria() {
		Criteria criteria = new Criteria().setClazz(Transaction.class).setOrderBy("DESC")
				.setOrderByField("transaction_time").setSelectColumn(Arrays.asList("*"));
		return criteria;
	}

	private void applyTransactionFilters(Criteria criteria, Map<String, Object> txMap) {
		DAOHelper.addConditionIfPresent(criteria, txMap, "customerId", "customer_id", "EQUAL_TO", 0L);
		DAOHelper.addConditionIfPresent(criteria, txMap, "from", "transaction_time", "GREATER_THAN", 0L);
		DAOHelper.addConditionIfPresent(criteria, txMap, "to", "transaction_time", "LESS_THAN", 0L);
		DAOHelper.addCondition(criteria, txMap.get("transactionType") != null, "transaction_type", "EQUAL_TO",
				txMap.get("transactionType"));
	}

	private Criteria applyBranchFilter(Criteria criteria, Map<String, Object> txMap) {
		if (!txMap.containsKey("branchId")) {
			return criteria;
		}
		criteria = DAOHelper.buildJoinCriteria(Transaction.class, Arrays.asList("branch"), new ArrayList<>(),
				new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), " JOIN ",
				true);
		criteria.setSelectColumn(Collections.singletonList("transaction.*"));
		DAOHelper.addJoinCondition(criteria, true, "transaction.ifsc", "EQUAL_TO", "branch.ifsc_code");
		DAOHelper.addCondition(criteria, true, "branch.id", "EQUAL_TO", txMap.get("branchId"));
		return criteria;
	}

	private void applyPagination(Criteria criteria, Map<String, Object> txMap) {
		Long limitValue = (Long) txMap.getOrDefault("limit", 0L);
		Long offset = (Long) txMap.getOrDefault("offset", -1L);

		if (limitValue > 0) {
			criteria.setLimitValue(limitValue);
		}
		if (offset >= 0) {
			criteria.setOffsetValue(offset == 0 ? -1L : offset);
		}
	}

	private Map<String, Object> executeQuery(Criteria criteria, Map<String, Object> txMap) throws CustomException {
		Map<String, Object> txResult = new HashMap<>();
		Long offset = (Long) txMap.getOrDefault("offset", -1L);
		try {
			if (offset == 0) {
				criteria.setOffsetValue(-1L).setAggregateFunction("COUNT").setAggregateOperator("*");
				txResult.put("count", SQLHelper.get(criteria).get(0));
				criteria.setOffsetValue(offset);
			}
			System.out.println(offset + " " + criteria);
			txResult.put("transactions", SQLHelper.get(criteria));
		} catch (SQLException e) {
			logger.error("Error while fetching transaction details: ", e);
			throw new CustomException("Failed to fetch transaction details: ", HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
		return txResult;
	}

}