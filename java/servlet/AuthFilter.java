package servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

import cache.CacheUtil;
import controller.OauthCallbackController;
import controller.OauthController;
import dao.DAO;
import dao.OauthProviderDAO;
import enums.Constants.HttpStatusCodes;
import enums.Constants.RolePermission;
import enums.Constants.Status;
import model.Account;
import model.OauthProvider;
import model.User;
import model.UserSession;
import service.AccountService;
import service.UserService;
import service.UserSessionService;
import util.CustomException;
import util.Helper;

@SuppressWarnings("serial")
public class AuthFilter extends HttpFilter implements Filter {

	private static Logger logger = LogManager.getLogger(AuthFilter.class);
	private DAO<OauthProvider> oauthProviderDAO = OauthProviderDAO.getInstance();

	@Override
	public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		String path = request.getPathInfo();
		String handler = Helper.getHandler(request);

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");

		logger.info("Request path: {}", path);

		if (isPublicEndpoint(path)) {
			chain.doFilter(request, response);
			return;
		}

		String sessionId = Helper.getFromCookies(request, "sessionId");
		if (sessionId == null) {
			Helper.sendJsonResponse(response, HttpStatusCodes.NOT_FOUND, "Authentication failed.", null);
			return;
		}

		logger.info("session found.");

		Map<String, Object> claimsMap = null;
		try {
			UserSession userSession = isValidSession(sessionId, request, response);
			
			claimsMap = Helper.getClaimsFromId(userSession.getUserId());
			if (claimsMap == null) {
				throw new IllegalArgumentException("Invalid session");
			}

		} catch (Exception e) {
			logger.error(e);
			Helper.sendJsonResponse(response, HttpStatusCodes.BAD_REQUEST, "Invalid session", null);
			return;
		}
		Long userId = (Long) claimsMap.get("id");
		String role = (String) claimsMap.get("role");

		Helper.setThreadLocalValue(claimsMap);

		if (!validateUser(userId, response)) {
			return;
		}
		if (!validateAccount(userId, response)) {
			return;
		}

		if (!handler.equals("oauth") && !checkPermissions(role, handler, request, response)) {
			return;
		}

		try {
			chain.doFilter(request, response);
		} finally {
			Helper.clearThreadLocal();
			logger.info("Cleared thread-local values.");
		}
	}

	private boolean isPublicEndpoint(String path) {
		if (path.equals("/Login") || path.contains("oauth")) {
			logger.info("Skipping authentication for login endpoint.");
			return true;
		}
		return false;
	}

	private UserSession isValidSession(String sessionId, HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		try {
			logger.info("validating session id...");

			UserSessionService userSessionService = UserSessionService.getInstance();
			Map<String, Object> sessionMap = new HashMap<>();
			sessionMap.put("sessionId", sessionId);

			List<UserSession> userSessions = userSessionService.getSessionDetails(sessionMap);
		
			if (userSessions == null || userSessions.isEmpty()) {
				throw new CustomException("Invalid session id, Please signin again", HttpStatusCodes.BAD_REQUEST);
			}

			UserSession userSession = userSessions.get(0);

			if (userSession.getProviderId() != null) {
				Map<String, Object> oauthProviderMap = new HashMap<>();
				oauthProviderMap.put("id", userSession.getProviderId());
				List<OauthProvider> oauthProviders = oauthProviderDAO.get(oauthProviderMap);

				if (oauthProviders.isEmpty()) {
					throw new CustomException("Invalid access token", HttpStatusCodes.BAD_REQUEST);
				}
				validateOAuthToken(oauthProviders.get(0).getAccessToken(), request, response);
			} else if (userSession.getExpiresAt() < System.currentTimeMillis()) {
				throw new CustomException("session expired", HttpStatusCodes.FORBIDDEN);
			}

			return userSessions.get(0);
		} catch (Exception e) {
			logger.error(e);
			Throwable cause = e.getCause();
			if (cause instanceof CustomException) {
				throw e;
			} else {
				throw new CustomException("Invalid session Id", HttpStatusCodes.BAD_REQUEST);
			}

		}
	}

	private Map<String, Object> validateOAuthToken(String token, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		OauthController oauthController = OauthController.getInstance();
		try {
			logger.info("Verifying Access token...");
			OauthCallbackController oauthCallbackController = OauthCallbackController.getInstance();
			String accessToken = oauthCallbackController.isValidOAuthToken(response, token);
			OauthProvider oauthProvider = oauthCallbackController.getOuathProvider(accessToken);
			return Helper.getClaimsFromId(oauthProvider.getUserId());
		} catch (CustomException e) {
			logger.warn("Signing in again due to invalid OAuth token.");
			try {
				request.setAttribute("token", token);
				oauthController.handleGet(request, response, "google");
			} catch (Exception exception) {
				logger.error(e);
				Helper.sendJsonResponse(response, HttpStatusCodes.BAD_REQUEST, "Invalid Access token", null);
			}
			return null;
		} catch (Exception e) {
			logger.error("OAuth validation failed.", e);
			Helper.sendJsonResponse(response, HttpStatusCodes.BAD_REQUEST, "Invalid Access token", null);
		}
		return null;
	}

	private boolean validateUser(Long userId, HttpServletResponse response) throws IOException {
		try {
			String key = "userExist" + userId;
			String cachedStatus = CacheUtil.get(key, new TypeReference<String>() {
			});

			if (cachedStatus != null) {
				boolean isActive = cachedStatus.contains("userActive: true");
				if (!isActive) {
					Helper.sendJsonResponse(response, HttpStatusCodes.FORBIDDEN, "You are not an active user", null);
				}
				return isActive;
			}

			UserService userService = UserService.getInstance();
			User user = userService.getUserById(userId);

			if (user == null) {
				Helper.sendJsonResponse(response, HttpStatusCodes.FORBIDDEN, "You are not an active user", null);
				CacheUtil.save(key, "userActive: false");
				return false;
			}

			boolean userActive = user.getStatusEnum() == Status.Active;
			CacheUtil.save(key, "userActive: " + userActive);
			if (!userActive) {
				Helper.sendJsonResponse(response, HttpStatusCodes.FORBIDDEN, "User is not active", null);
			}

			return userActive;

		} catch (Exception e) {
			logger.error("Error fetching user details.", e);
			Helper.sendJsonResponse(response, HttpStatusCodes.INTERNAL_SERVER_ERROR, "Error fetching user details.",
					null);
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	private boolean validateAccount(Long userId, HttpServletResponse response) throws IOException {
		try {
			String key = "userAccountExist" + userId;
			String cachedStatus = CacheUtil.get(key, new TypeReference<String>() {
			});

			if (cachedStatus != null) {
				boolean isActive = cachedStatus.contains("accountActive: true");
				if (!isActive) {
					Helper.sendJsonResponse(response, HttpStatusCodes.FORBIDDEN, "You don't have an active account",
							null);
				}
				return isActive;
			}

			AccountService accountService = AccountService.getInstance();
			Map<String, Object> accountMap = new HashMap<>();
			accountMap.put("userId", userId);
			Map<String, Object> accountResult = accountService.getAccountDetails(accountMap);

			if (!accountResult.containsKey("accounts") || ((List<Account>) accountResult.get("accounts")).isEmpty()) {
				Helper.sendJsonResponse(response, HttpStatusCodes.FORBIDDEN, "You don't have an account", null);
				CacheUtil.save(key, "accountActive: false");
				return false;
			}

			List<Account> accounts = (List<Account>) accountResult.get("accounts");
			boolean accountActive = accounts.stream().allMatch(account -> account.getStatusEnum() == Status.Active);
			CacheUtil.save(key, "accountActive: " + accountActive);
			if (!accountActive) {
				Helper.sendJsonResponse(response, HttpStatusCodes.FORBIDDEN, "Your account is not active", null);
			}

			return accountActive;

		} catch (Exception e) {
			logger.error("Error fetching account details.", e);
			Helper.sendJsonResponse(response, HttpStatusCodes.INTERNAL_SERVER_ERROR, "Error fetching account details.",
					null);
			return false;
		}
	}

	private boolean checkPermissions(String role, String handler, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		List<String> allowedMethods = RolePermission.getPermissions(role, handler);

		if (!allowedMethods.contains(request.getMethod())) {
			logger.warn("User with role {} does not have permission to access {} with method {}", role, handler,
					request.getMethod());
			Helper.sendJsonResponse(response, HttpStatusCodes.FORBIDDEN, "Permission Denied.", null);
			return false;
		}
		return true;
	}
}
