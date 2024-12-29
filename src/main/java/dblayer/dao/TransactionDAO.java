package dblayer.dao;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
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
		Criteria criteria = new Criteria();
		criteria.setClazz(Transaction.class);
		criteria.setOrderBy("DESC");
		criteria.setOrderByField("transaction_time");
		criteria.setSelectColumn(Arrays.asList("*"));

		Helper.addConditionIfPresent(criteria, txMap, "id", "customer_id", "=", 0L);
		Helper.addConditionIfPresent(criteria, txMap, "from", "transaction_time", ">", 0L);
		Helper.addConditionIfPresent(criteria, txMap, "to", "transaction_time", "<", 0L);
		Helper.addCondition(criteria, txMap.get("transactionType") != null, "transaction_type", "=",
				txMap.get("transactionType"));
		Helper.addCondition(criteria, (Long) txMap.get("accountNumber") > 0, "account_number", "LIKE",
				"%" + txMap.get("accountNumber") + "%");

		Long limitValue = (Long) txMap.getOrDefault("limit", 0L);
		Long offset = (Long) txMap.getOrDefault("offset", -1L);

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
		txResult.put("transactions", SQLHelper.get(criteria));
		return txResult;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getTransactionByBranchId(List<Account> accounts, Map<String, Object> txMap)
			throws CustomException {
		logger.info("Checking transactions by branch ID...");
		Map<String, Object> transactions = new HashMap<>();
		transactions.put("transactions", new ArrayList<Transaction>());
		Long branchId = (Long) Helper.getThreadLocalValue().get("branchId");
		for (Account account : accounts) {
			if (account.getBranchId() == branchId) {
				try {
					txMap.put("id", 0l);
					txMap.put("accountNumber", account.getAccountNumber());
					Map<String, Object> accountTransactions = getTransactions(txMap);
					transactions.put("count", ((Long) transactions.getOrDefault("count", 0l))
							+ ((Long) accountTransactions.getOrDefault("count", 0l)));
					((List<Transaction>) transactions.get("transactions"))
							.addAll((List<Transaction>) accountTransactions.get("transactions"));
				} catch (CustomException e) {
					logger.log(Level.SEVERE, "Error fetching transactions for account: " + account.getAccountNumber(),
							e);
					throw e;
				}
			} else {
				logger.info("Skipping account due to branch mismatch: " + account.getAccountNumber());
			}
		}
		return transactions;
	}

}