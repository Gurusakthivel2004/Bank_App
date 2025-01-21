package service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Enum.Constants.HttpStatusCodes;
import Enum.Constants.Role;

import dao.AccountDAO;
import dao.DAO;

import model.Account;
import model.Branch;
import model.MarkedClass;
import model.Transaction;
import model.User;

import util.CustomException;
import util.Helper;
import util.ValidationUtil;

public class AuthorizationService {

	private Long userId = (Long) Helper.getThreadLocalValue("id");
	private Long branchId = (Long) Helper.getThreadLocalValue("branchId");

	public <T extends MarkedClass> boolean isAuthorized(String entityType, List<T> data) throws CustomException {
		switch (entityType) {
		case "branch":
			return isBranchAuthorized(data);
		case "account":
			return isAccountAuthorized(data);
		case "user":
			return isUserAuthorized(data);
		case "transaction":
			return isTransactionAuthorized(data);
		default:
			return false;
		}
	}

	public boolean isBranchAuthorized(List<? extends MarkedClass> branches) throws CustomException {

		if (branches.isEmpty() || !(branches.get(0) instanceof Branch)) {
			throw new CustomException("Invalid branch data", HttpStatusCodes.BAD_REQUEST);
		}

		Long branchId = ((Branch) branches.get(0)).getId();
		Role role = Role.fromString((String) Helper.getThreadLocalValue("role"));
		switch (role) {
		case Customer:
			DAO<Account> accountDAO = new AccountDAO();
			Map<String, Object> accountMap = new HashMap<>();

			accountMap.put("userId", userId);
			List<Account> accounts = accountDAO.get(accountMap);

			if (accounts == null) {
				throw new CustomException("No accounts found for user", HttpStatusCodes.BAD_REQUEST);
			}
			return ValidationUtil.getAssignedBranches(accounts, branchId);
		case Manager:
		case Employee:
			return true;
		default:
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	public boolean isAccountAuthorized(List<? extends MarkedClass> accounts) throws CustomException {

		if (!accounts.isEmpty() && !(accounts.get(0) instanceof Account)) {
			throw new CustomException("Invalid Account data", HttpStatusCodes.BAD_REQUEST);
		}
		Role role = Role.fromString((String) Helper.getThreadLocalValue("role"));
		switch (role) {
		case Customer:
			return accounts.stream().allMatch(account -> ((Account) account).getUserId() == userId);
		case Employee:
			return ValidationUtil.getAssignedBranches((List<Account>) accounts, branchId);
		case Manager:
			return true;
		default:
			return false;
		}

	}

	public boolean isUserAuthorized(List<? extends MarkedClass> users) throws CustomException {
		if (!users.isEmpty() && !(users.get(0) instanceof User)) {
			throw new CustomException("Invalid User data", HttpStatusCodes.BAD_REQUEST);
		}
		Role role = Role.fromString((String) Helper.getThreadLocalValue("role"));
		switch (role) {
		case Customer:
			return users.stream().allMatch(user -> ((User) user).getId() == userId);
		case Manager:
		case Employee:
			return true;
		default:
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	public boolean isTransactionAuthorized(List<? extends MarkedClass> transactions) throws CustomException {
		if (!transactions.isEmpty() && !(transactions.get(0) instanceof Transaction)) {
			throw new CustomException("Invalid transaction data", HttpStatusCodes.BAD_REQUEST);
		}
		Role role = Role.fromString((String) Helper.getThreadLocalValue("role"));
		switch (role) {
		case Customer:
			return transactions.stream().allMatch(transaction -> ((Transaction) transaction).getCustomerId() == userId);
		case Employee:
			return ValidationUtil.getAssignedTransactions((List<Transaction>) transactions, branchId);
		case Manager:
			return true;
		default:
			return false;
		}
	}
}
