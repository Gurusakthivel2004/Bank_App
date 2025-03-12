package controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonObject;

import cache.CacheUtil;
import enums.Constants.HttpStatusCodes;
import enums.Constants.LogType;
import enums.Constants.Role;
import model.ActivityLog;
import model.UserSession;
import service.TaskExecutorService;
import service.UserService;
import service.UserSessionService;
import util.AuthUtils;
import util.CustomException;
import util.Helper;

public class LoginController {

	private UserService userService = UserService.getInstance();
	private UserSessionService userSessionService = UserSessionService.getInstance();

	private LoginController() {
	}

	private static class SingletonHelper {
		private static final LoginController INSTANCE = new LoginController();
	}

	public static LoginController getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public void handlePost(HttpServletRequest request, HttpServletResponse response) throws Exception {

		JsonObject jsonObject = Helper.parseRequestBody(request);
		Map<String, Object> loginMap = Helper.mapJsonObject(jsonObject);

		String username = (String) loginMap.get("username");
		String encryptedPassword = (String) loginMap.get("password");
		String iv = (String) loginMap.get("iv");
		String password = Helper.decryptAES(encryptedPassword, iv);

		Map<String, Object> userDetails = new HashMap<>();

		try {
			userDetails = userService.userLogin(username, password);
		} catch (CustomException e) {
			AuthUtils.handleFailedAttempt(username);
			throw e;
		}

		String sessionId = UUID.randomUUID().toString();
		Long userId = (Long) userDetails.get("id");

		checkActiveSessions(userId);

		setCookies(response, userDetails, sessionId);
		saveSessionId(sessionId, userId, null);

		Map<String, Object> responseData = new HashMap<>();
		responseData.put("message", "success");

		Helper.sendSuccessResponse(response, responseData);

	}

	public void handleDelete(HttpServletRequest request, HttpServletResponse response) throws Exception {
		Long userId = (Long) Helper.getThreadLocalValue("id");

		String sessionId = Helper.getFromCookies(request, "sessionId");

		Cookie cookie = new Cookie("sessionId", "");
		cookie.setMaxAge(0);
		cookie.setPath("/Bank_Application");
		response.addCookie(cookie);

		CacheUtil.delete(sessionId);
		Map<String, Object> sessionMap = new HashMap<>();
		sessionMap.put("sessionId", sessionId);
		userSessionService.deleteSession(sessionMap);
		
		ActivityLog activityLog = new ActivityLog().setLogMessage("Logout").setLogType(LogType.Logout)
				.setUserAccountNumber(null).setRowId(userId).setTableName("User").setUserId(userId);

		TaskExecutorService.getInstance().submit(activityLog);
		Helper.sendSuccessResponse(response, "Logout successful");
	}

	private void checkActiveSessions(Long userId) throws Exception {
		UserSessionService userSessionService = UserSessionService.getInstance();
		Map<String, Object> sessionMap = new HashMap<>();
		sessionMap.put("userId", userId);

		List<UserSession> userSessions = userSessionService.getSessionDetails(sessionMap);

		if (userSessions != null && !userSessions.isEmpty()) {
			for (UserSession userSession : userSessions) {
				if (System.currentTimeMillis() < userSession.getExpiresAt()) {
					throw new CustomException("Active session exists, Please signin later",
							HttpStatusCodes.BAD_REQUEST);
				}
			}
		}
	}

	public void saveSessionId(String sessionId, Long userId, Long providerId) throws Exception {
		Map<String, Object> sessionMap = new HashMap<>();
		sessionMap.put("sessionId", sessionId);
		sessionMap.put("userId", userId);
		sessionMap.put("providerId", providerId);
		sessionMap.put("createdAt", System.currentTimeMillis());
		sessionMap.put("expiresAt", System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1));

		userSessionService.createSession(sessionMap);
		CacheUtil.saveWithTTL(sessionId, "true", 3600);
	}

	private void setCookies(HttpServletResponse response, Map<String, Object> userDetails, String sessionId)
			throws CustomException {
		Role role = Role.fromString((String) userDetails.get("role"));
		Long userId = (Long) userDetails.get("id");

		Helper.setCookie(response, "phone", userDetails.get("phone"), 604800, false);
		Helper.setCookie(response, "email", userDetails.get("email"), 604800, false);
		Helper.setCookie(response, "sessionId", sessionId, 604800, true);
		Helper.setCookie(response, "fullname", userDetails.get("fullname"), 604800, false);
		Helper.setCookie(response, "status", userDetails.get("status"), 604800, false);
		Helper.setCookie(response, "role", userDetails.get("role"), 604800, false);
		Helper.setCookie(response, "id", userId, 604800, false);

		if (role != Role.Customer) {
			Helper.setCookie(response, "branchId", userDetails.get("branchId"), 604800, false);
		}
	}

}
