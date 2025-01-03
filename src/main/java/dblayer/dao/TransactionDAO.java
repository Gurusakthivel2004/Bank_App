package dblayer.dao;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import dblayer.model.Account;
import dblayer.model.ColumnCriteria;
import dblayer.model.Criteria;
import dblayer.model.Transaction;
import util.CustomException;
import util.Helper;
import util.SQLHelper;

public class TransactionDAO {

	private static final Logger logger = Logger.getLogger(TransactionDAO.class.getName());

	public void updateAccountBalance(Long accountNumber, BigDecimal balance) throws CustomException {
		logger.info("Updating account balance for account: " + accountNumber);
		ColumnCriteria columnCriteria = new ColumnCriteria();
		columnCriteria.setFields(Arrays.asList("balance"));
		columnCriteria.setValues(Arrays.asList(balance));

		Criteria criteria = new Criteria();
		criteria.setClazz(Account.class);
		criteria.getColumn().add("account_number");
		criteria.getOperator().add("=");
		criteria.getValue().add(accountNumber);

		SQLHelper.update(columnCriteria, criteria);
		logger.info("Account balance updated successfully.");
	}

	public Long createTransaction(Transaction transaction) throws CustomException {
		Long txId = ((BigInteger) SQLHelper.insert(transaction)).longValue();
		return txId;
	}

	public void removeFailedTransaction(Criteria criteria) throws CustomException {
		SQLHelper.delete(criteria);
	}

	public Map<String, Object> getTransactions(Map<String, Object> txMap) throws CustomException {
		logger.info("Fetching transactions with provided filters...");

		Criteria criteria = initializeCriteria();
		criteria = applyBranchFilter(criteria, txMap);
		applyTransactionFilters(criteria, txMap);
		applyAccountNumberFilter(criteria, txMap);
		
		applyPagination(criteria, txMap);

		return executeQuery(criteria, txMap);
	}

	private Criteria initializeCriteria() {
		Criteria criteria = new Criteria();
		criteria.setClazz(Transaction.class);
		criteria.setOrderBy("DESC");
		criteria.setOrderByField("transaction_time");
		criteria.setSelectColumn(Arrays.asList("*"));
		return criteria;
	}

	private void applyTransactionFilters(Criteria criteria, Map<String, Object> txMap) {
		Helper.addConditionIfPresent(criteria, txMap, "customerId", "customer_id", "=", 0L);
		Helper.addConditionIfPresent(criteria, txMap, "from", "transaction_time", ">", 0L);
		Helper.addConditionIfPresent(criteria, txMap, "to", "transaction_time", "<", 0L);
		Helper.addCondition(criteria, txMap.get("transactionType") != null, "transaction_type", "=",
				txMap.get("transactionType"));
	}

	private void applyAccountNumberFilter(Criteria criteria, Map<String, Object> txMap) {
		Long accountNumber = (Long) txMap.get("accountNumber");
		if (accountNumber != null && accountNumber > 0) {
			if (accountNumber <= 9999) {
				Helper.addCondition(criteria, true, "RIGHT(account_number, 4)", "=", accountNumber);
			} else {
				Helper.addConditionIfPresent(criteria, txMap, "accountNumber", "account_number", "=", 0L);
			}
		}
	}

	private Criteria applyBranchFilter(Criteria criteria, Map<String, Object> txMap) {
		if (!txMap.containsKey("branchId")) {
			return criteria;
		}
		criteria = Helper.buildJoinCriteria(Transaction.class, Arrays.asList("branch"), new ArrayList<>(),
				new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), true);
		criteria.setSelectColumn(Collections.singletonList("transaction.*"));
		Helper.addJoinCondition(criteria, true, "transaction.ifsc", "=", "branch.ifsc_code");
		Helper.addCondition(criteria, true, "branch.id", "=", txMap.get("branchId"));
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
		if (offset == 0) {
			criteria.setOffsetValue(-1L);
			txResult.put("count", SQLHelper.get(criteria).get(0));
			criteria.setOffsetValue(offset);
		}
		txResult.put("transactions", SQLHelper.get(criteria));
		return txResult;
	}

}