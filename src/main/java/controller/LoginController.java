package controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonObject;

import cache.CacheUtil;
import enums.Constants.LogType;
import enums.Constants.Role;
import model.ActivityLog;
import service.TaskExecutorService;
import service.UserService;
import util.AuthUtils;
import util.CustomException;
import util.Helper;

public class LoginController {

	private UserService userService = UserService.getInstance();
	
	private LoginController() {}

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

		String jwtToken = Helper.generateJwtToken(userDetails);
		Role role = Role.fromString((String) userDetails.get("role"));
		Long userId = (Long) userDetails.get("id");

		Helper.setCookie(response, "phone", userDetails.get("phone"), 604800, false);
		Helper.setCookie(response, "email", userDetails.get("email"), 604800, false);
		Helper.setCookie(response, "token", jwtToken, 604800, true);
		Helper.setCookie(response, "fullname", userDetails.get("fullname"), 604800, false);
		Helper.setCookie(response, "status", userDetails.get("status"), 604800, false);
		Helper.setCookie(response, "role", userDetails.get("role"), 604800, false);
		Helper.setCookie(response, "id", userId, 604800, false);

		if (role != Role.Customer) {
			Helper.setCookie(response, "branchId", userDetails.get("branchId"), 604800, false);
		}

		CacheUtil.saveWithTTL(userId.toString(), jwtToken, 3600);
		Map<String, Object> responseData = new HashMap<>();
		responseData.put("message", "success");

		Helper.sendSuccessResponse(response, responseData);

	}

	public void handleDelete(HttpServletRequest request, HttpServletResponse response) throws Exception {
		Long userId = (Long) Helper.getThreadLocalValue("id");

		Cookie cookie = new Cookie("token", "");
		cookie.setMaxAge(0);
		cookie.setPath("/Bank_Application");
		response.addCookie(cookie);

		CacheUtil.delete(userId.toString());
		ActivityLog activityLog = new ActivityLog().setLogMessage("Logout").setLogType(LogType.Logout)
				.setUserAccountNumber(null).setRowId(userId).setTableName("User").setUserId(userId);

		TaskExecutorService.getInstance().submit(activityLog);
		Helper.sendSuccessResponse(response, "Logout successful");
	}

}
