package util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import dao.AccountDAO;
import dao.DAO;
import enums.Constants.HttpStatusCodes;
import enums.Constants.Role;
import model.Account;
import model.Branch;
import model.MarkedClass;
import model.User;

public class AuthUtils {

	private static int ATTEMPTS_LIMIT = 4;
	private static int TIMEOUT_SECONDS = 300;
	private static int CAPTCHA_TRIGGER_LIMIT = 2;
	private static Map<String, Integer> failedAttemptsMap = new HashMap<>();
	private static Map<String, Long> lockoutTimestampMap = new HashMap<>();
	private static Long userId = (Long) Helper.getThreadLocalValue("id");
	private static Long branchId = (Long) Helper.getThreadLocalValue("branchId");

	public static <T extends MarkedClass> boolean isAuthorized(String entityType, List<T> data) throws Exception {
		switch (entityType) {
		case "branch":
			return isBranchAuthorized(data);
		case "account":
			return isAccountAuthorized(data);
		case "user":
			return isUserAuthorized(data);
		default:
			return false;
		}
	}

	public static void handleFailedAttempt(String username) {
		int attempts = failedAttemptsMap.getOrDefault(username, 0) + 1;
		failedAttemptsMap.put(username, attempts);

		if (attempts >= ATTEMPTS_LIMIT) {
			lockoutTimestampMap.put(username, System.currentTimeMillis());
		}
	}

	public static boolean isCaptchaRequired(String username, HttpSession session) {
		return session.getAttribute("captchaVerified") == null
				&& failedAttemptsMap.getOrDefault(username, 0) >= CAPTCHA_TRIGGER_LIMIT;
	}

	public static void resetFailedAttempts(String username) {
		failedAttemptsMap.remove(username);
		lockoutTimestampMap.remove(username);
	}

	public static boolean isUserLockedOut(String username) {
		if (lockoutTimestampMap.containsKey(username)) {
			long lockoutTime = lockoutTimestampMap.get(username);
			if ((System.currentTimeMillis() - lockoutTime) / 1000 >= TIMEOUT_SECONDS) {
				resetFailedAttempts(username);
				return false;
			}
			return true;
		}
		return false;
	}

	public static boolean isBranchAuthorized(List<? extends MarkedClass> branches) throws Exception {

		if (branches.isEmpty() || !(branches.get(0) instanceof Branch)) {
			throw new CustomException("Invalid branch data", HttpStatusCodes.BAD_REQUEST);
		}

		Long branchId = ((Branch) branches.get(0)).getId();
		Role role = Role.fromString((String) Helper.getThreadLocalValue("role"));
		switch (role) {
		case Customer:
			DAO<Account> accountDAO = AccountDAO.getInstance();
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
	public static boolean isAccountAuthorized(List<? extends MarkedClass> accounts) throws CustomException {
		if (!accounts.isEmpty() && !(accounts.get(0) instanceof Account)) {
			throw new CustomException("Invalid Account data", HttpStatusCodes.BAD_REQUEST);
		}
		Role role = Role.fromString((String) Helper.getThreadLocalValue("role"));
		switch (role) {
		case Customer:
			System.out.println(accounts);
			return accounts.stream().allMatch(account -> ((Account) account).getUserId() == userId);
		case Employee:
			System.out.println(accounts);
			return ValidationUtil.getAssignedBranches((List<Account>) accounts, branchId);
		case Manager:
			return true;
		default:
			return false;
		}
	}

	public static boolean isUserAuthorized(List<? extends MarkedClass> users) throws CustomException {
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
}
