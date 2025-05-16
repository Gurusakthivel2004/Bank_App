package dao;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import model.ColumnCriteria;
import model.Criteria;
import model.Transaction;
import util.Helper;
import util.SQLHelper;

public class TransactionDAO implements DAO<Transaction> {

	private static Logger logger = LogManager.getLogger(TransactionDAO.class);

	private TransactionDAO() {
	}

	private static class SingletonHelper {
		private static final TransactionDAO INSTANCE = new TransactionDAO();
	}

	public static TransactionDAO getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public long create(Transaction transaction) throws Exception {
		Object insertedValue = SQLHelper.insert(transaction);
		return Helper.convertToLong(insertedValue);
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
		logger.info("Updating transaction details {}", txMap);
		Criteria criteria = new Criteria().setClazz(Transaction.class);
		DAOHelper.addConditionIfPresent(criteria, txMap, "id", "id", "EQUAL_TO", 0l);
		SQLHelper.update(columnCriteria, criteria);
	}
}