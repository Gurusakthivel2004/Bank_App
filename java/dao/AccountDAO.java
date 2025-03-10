package dao;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import model.Account;
import model.ColumnCriteria;
import model.Criteria;
import model.JoinObject;
import util.CustomException;
import util.SQLHelper;

public class AccountDAO implements DAO<Account>, DAOJoin<Account> {

	private static Logger logger = LogManager.getLogger(AccountDAO.class);

	private AccountDAO() {}

	private static class SingletonHelper {
		private static final AccountDAO INSTANCE = new AccountDAO();
	}

	public static AccountDAO getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public void update(ColumnCriteria columnCriteria, Map<String, Object> accountMap) throws Exception {
		logger.info("Updating account details {}", accountMap);
		Criteria criteria = new Criteria().setClazz(Account.class);
		DAOHelper.addConditionIfPresent(criteria, accountMap, "accountNumber", "account_number", "EQUAL_TO", 0l);
		DAOHelper.addConditionIfPresent(criteria, accountMap, "accountId", "account_id", "EQUAL_TO", 0l);

		SQLHelper.update(columnCriteria, criteria);
	}

	public List<Account> get(Map<String, Object> accountMap) throws CustomException, SQLException {
		logger.info("Fetching account data");
		Criteria criteria = DAOHelper.getAccountCriteria(accountMap);
		return SQLHelper.get(criteria, Account.class);
	}

	public long getDataCount(Map<String, Object> accountMap) throws Exception {
		Criteria criteria = DAOHelper.getAccountCriteria(accountMap);
		criteria.setOffsetValue(-1L).setAggregateFunction("COUNT").setAggregateOperator("*");
		long count = SQLHelper.getCount(criteria, Account.class);

		return count;
	}

	public List<JoinObject<Account>> getJoined(Map<String, Object> accountMap) throws Exception {
		Criteria branchJoinCriteria = DAOHelper.buildJoinCriteria(Account.class, Arrays.asList("branch"),
				new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
				new ArrayList<>(), " JOIN ", true);
		branchJoinCriteria.setSelectColumn(Arrays.asList("account.*", "branch.name"));

		DAOHelper.addJoinCondition(branchJoinCriteria, true, "account.branch_id", "EQUAL_TO", "branch.id");
		DAOHelper.applyAccountFilters(branchJoinCriteria, accountMap);
		DAOHelper.applyPagination(branchJoinCriteria, accountMap);

		return SQLHelper.getJoinedObjects(branchJoinCriteria, Account.class);
	}

	public long create(Account account) throws Exception {
		logger.info("Creating account: {}", account);

		account.setCreatedAt(System.currentTimeMillis());

		Long accountId = ((BigInteger) SQLHelper.insert(account)).longValue();
		logger.debug("Account ID generated: {}", accountId);

		ColumnCriteria columnCriteria = new ColumnCriteria().setFields(Arrays.asList("accountNumber"));
		String accountNumber = "701" + String.format("%04d", account.getBranchId()) + String.format("%06d", accountId);

		columnCriteria.setValues(Arrays.asList(Long.parseLong(accountNumber)));

		Criteria criteria = new Criteria().setClazz(Account.class);
		criteria.getColumn().add("account_id");
		criteria.getOperator().add("EQUAL_TO");
		criteria.getValue().add(accountId);

		Map<String, Object> accountMap = new HashMap<>();
		accountMap.put("accountId", accountId);
		update(columnCriteria, accountMap);
		return accountId;
	}

}
