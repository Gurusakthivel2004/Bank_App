package dblayer.dao;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

	public List<Transaction> getTransactions(Long customerId, Long accountNumber, Long limitValue, Long from, Long to)
			throws CustomException {
		logger.info("Fetching transactions with provided filters...");
		Criteria criteria = new Criteria();
		criteria.setClazz(Transaction.class);
		criteria.setOrderBy("DESC");
		criteria.setOrderByField("transaction_time");
		criteria.setSelectColumn(Arrays.asList("*"));

		Helper.addCondition(criteria, customerId > 0, "customer_id", "=", customerId);
		Helper.addCondition(criteria, accountNumber > 0, "account_number", "=", accountNumber);
		Helper.addCondition(criteria, from > 0, "transaction_time", ">", from);
		Helper.addCondition(criteria, to > 0, "transaction_time", "<", to);

		if (limitValue > 0) {
			criteria.setLimitValue(limitValue);
		}
		if (criteria.getColumn().size() > 1) {
			criteria.setLogicalOperator("AND");
		}

		logger.info("Criteria for transactions: " + criteria);
		return SQLHelper.get(criteria);
	}
	
	// returns the list of transactions in the branch
	public List<Transaction> checkTransactionBranchId(List<Account> accounts, Long from, Long to, Long limit)
			throws CustomException {
		logger.info("Checking transactions by branch ID...");
		List<Transaction> transactions = new ArrayList<>();
		Long branchId = (Long) Helper.getThreadLocalValue().get("branchId");
		for (Account account : accounts) {
			if (account.getBranchId() == branchId) {
				try {

					List<Transaction> accountTransactions = getTransactions(0l, account.getAccountNumber(), limit, from,
							to);
					transactions.addAll(accountTransactions);
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

	public void makeTransaction(Transaction transaction, boolean thisBank) throws CustomException {
		logger.info("Starting transaction processing...");
		Long txId = createTransaction(transaction);
		if (thisBank) {
			try {
				AccountDAO accountDAO = new AccountDAO();
				Long accountNumber = transaction.getAccountNumber();
				Long transactionAccountNumber = transaction.getTransactionAccountNumber();
				String transactionIfsc = transaction.getTransactionIfsc();
				String ifsc = transaction.getIfsc();
				List<Account> accounts = accountDAO.getAccounts(0l, transactionAccountNumber, 0l, 0l, 0l);
				Helper.checkNullValues(accounts);

				String transactionType = transaction.getTransactionType();
				Long transactionUserId = accounts.get(0).getUserId();

				transaction.setIfsc(transactionIfsc);
				transaction.setTransactionIfsc(ifsc);
				transaction.setAccountNumber(transactionAccountNumber);
				transaction.setTransactionAccountNumber(accountNumber);
				transaction.setCustomerId(transactionUserId);
				transaction.setId(txId);

				if ("Debit".equals(transactionType)) {
					transaction.setTransactionType("Credit");
				}

				createTransaction(transaction);
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Error during intra-bank transaction, rolling back...", e);
				Criteria criteria = new Criteria();
				criteria.setClazz(Transaction.class);
				criteria.setColumn(new ArrayList<>(Arrays.asList("id")));
				criteria.setOperator(new ArrayList<>(Arrays.asList("=")));
				criteria.setValue(new ArrayList<>(Arrays.asList(txId)));
				SQLHelper.delete(criteria);
			}
		}
	}

	public Long createTransaction(Transaction transaction) throws CustomException {
		logger.info("Creating a new transaction...");

		AccountDAO accountDAO = new AccountDAO();
		transaction.setStatus("Completed");
		transaction.setTransactionTime(System.currentTimeMillis());
		transaction.setPerformedBy((Long) Helper.getThreadLocalValue().get("id"));

		String transactionType = transaction.getTransactionType();
		BigDecimal amount = transaction.getAmount();
		List<Account> accounts = accountDAO.getAccounts(0l, transaction.getAccountNumber(), 0l, 0l, 0l);
		Helper.checkNullValues(accounts);

		BigDecimal accountBalance = accounts.get(0).getBalance();
		BigDecimal closingBalance;

		switch (transactionType) {
		case "Credit":
			closingBalance = accountBalance.add(amount);
			break;
		case "Withdraw":
		case "Debit":
			closingBalance = accountBalance.subtract(amount);
			break;
		case "Deposit":
			closingBalance = computeDepositBalance(accountBalance, transaction, accountDAO);
			break;
		default:
			logger.severe("Invalid transaction type: " + transactionType);
			throw new CustomException("Invalid transaction type: " + transactionType);
		}

		transaction.setClosingBalance(closingBalance);
		logger.info("Transaction details: " + transaction);
		validateTransaction(transaction);
		Long txId = ((BigInteger) SQLHelper.insert(transaction)).longValue();
		logger.info("Transaction created with ID: " + txId);

		updateAccountBalance(transaction.getAccountNumber(), closingBalance);
		return txId;
	}

	private BigDecimal computeDepositBalance(BigDecimal accountBalance, Transaction transaction, AccountDAO accountDAO)
			throws CustomException {
		logger.info("Computing deposit balance...");
		Account account = accountDAO.getAccounts(0l, transaction.getAccountNumber(), 0l, 0l, 0l).get(0);
		if (!"Operational".equals(account.getAccountType())) {
			return accountBalance.add(transaction.getAmount());
		} else {
			return accountBalance.subtract(transaction.getAmount());
		}
	}

	private void updateAccountBalance(Long accountNumber, BigDecimal balance) throws CustomException {
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

	public static void validateTransaction(Transaction transaction) throws CustomException {
		if (transaction == null) {
			throw new CustomException("Transaction object cannot be null.");
		}
		StringBuilder errorMessages = new StringBuilder();
		for (Field field : Transaction.class.getDeclaredFields()) {
			field.setAccessible(true);
			if(field.getName().equals("id")) {
				continue;
			}
			try {
				Object value = field.get(transaction);
				if (value == null || (value instanceof String && ((String) value).trim().isEmpty())
						|| (value instanceof BigDecimal && ((BigDecimal) value).compareTo(BigDecimal.ZERO) <= 0)) {
					errorMessages.append("Invalid value for field: ").append(field.getName()).append("\n");
				}
			} catch (IllegalAccessException e) {
				throw new CustomException("Error accessing field: " + field.getName(), e);
			}
		}
		if (errorMessages.length() > 0) {
			throw new CustomException("Validation failed:\n" + errorMessages.toString());
		}
	}
}
