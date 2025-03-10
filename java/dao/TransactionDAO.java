package dao;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import model.ColumnCriteria;
import model.Criteria;
import model.Transaction;
import util.SQLHelper;

public class TransactionDAO implements DAO<Transaction> {
	
	private TransactionDAO() {}

	private static class SingletonHelper {
		private static final TransactionDAO INSTANCE = new TransactionDAO();
	}

	public static TransactionDAO getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public long create(Transaction transaction) throws Exception {
		long txId;
		txId = ((BigInteger) SQLHelper.insert(transaction)).longValue();
		return txId;
	}

	public List<Transaction> get(Map<String, Object> txMap) throws Exception {
		Criteria criteria = getCriteria(txMap);
		return SQLHelper.get(criteria, Transaction.class);

	}

	private Criteria getCriteria(Map<String, Object> txMap) throws Exception {
		Criteria criteria = new Criteria().setClazz(Transaction.class).setSelectColumn(Arrays.asList("*"));
		criteria = DAOHelper.applyTransactionFilterBranch(criteria, txMap);
		DAOHelper.applyTransactionFilters(criteria, txMap);
		DAOHelper.applyAccountNumberFilter(criteria, txMap, "transaction");
		DAOHelper.applyPagination(criteria, txMap);
		criteria.setOrderBy("DESC").setOrderByField("transaction_time");
		return criteria;
	}

	public long getDataCount(Map<String, Object> txMap) throws Exception {
		Criteria criteria = getCriteria(txMap);
		criteria.setOffsetValue(-1L).setAggregateFunction("COUNT").setAggregateOperator("*");
		long count = SQLHelper.getCount(criteria, Transaction.class);
		return count;
	}

	public void update(ColumnCriteria columnCriteria, Map<String, Object> txMap) throws Exception {

		Criteria criteria = new Criteria().setClazz(Transaction.class);
		criteria.setSelectColumn(Arrays.asList("status"))
				.setValue(new ArrayList<>(Arrays.asList(txMap.get("status").toString())));
		DAOHelper.addCondition(criteria, txMap.containsKey("id"), "id", "EQUAL_TO", txMap.get("id"));

		SQLHelper.update(columnCriteria, criteria);

	}
}