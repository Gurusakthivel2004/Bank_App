package dao;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import Enum.Constants.HttpStatusCodes;
import model.ColumnCriteria;
import model.Criteria;
import model.Transaction;
import util.CustomException;
import util.SQLHelper;

public class TransactionDAO implements DAO<Transaction> {

	private final Logger logger = LogManager.getLogger(TransactionDAO.class);

	public Long create(Transaction transaction) throws CustomException {
		Long txId;
		try {
			txId = ((BigInteger) SQLHelper.insert(transaction)).longValue();
		} catch (Exception e) {
			logger.error("Error during inserting data: ", e);
			throw new CustomException("Error during creating transaction: ", HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
		return txId;
	}

	public List<Transaction> get(Map<String, Object> txMap) throws CustomException {
		Criteria criteria = getCriteria(txMap);
		try {
			return SQLHelper.get(criteria, Transaction.class);
		} catch (SQLException e) {
			logger.error("Error while fetching transaction details: ", e);
			throw new CustomException("Failed to fetch transaction details: ", HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

	private Criteria getCriteria(Map<String, Object> txMap) throws CustomException {
		Criteria criteria = new Criteria().setClazz(Transaction.class).setOrderBy("DESC")
				.setOrderByField("transaction_time").setSelectColumn(Arrays.asList("*"));
		criteria = DAOHelper.applyTransactionFilterBranch(criteria, txMap);
		DAOHelper.applyTransactionFilters(criteria, txMap);
		DAOHelper.applyAccountNumberFilter(criteria, txMap);
		DAOHelper.applyPagination(criteria, txMap);

		System.out.println(criteria);
		return criteria;
	}

	public Long getDataCount(Map<String, Object> txMap) throws CustomException {
		try {
			Criteria criteria = getCriteria(txMap);
			criteria.setOffsetValue(-1L).setAggregateFunction("COUNT").setAggregateOperator("*");
			Long count = SQLHelper.getCount(criteria, Transaction.class);
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

	public void update(ColumnCriteria columnCriteria, Map<String, Object> map) throws CustomException {
		throw new CustomException("Transaction cannot be altered.", HttpStatusCodes.INTERNAL_SERVER_ERROR);
	}
}