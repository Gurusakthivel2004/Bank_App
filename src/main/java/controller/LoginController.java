package controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonObject;

import Enum.Constants.LogType;
import cache.CacheUtil;
import model.ActivityLog;
import service.TaskExecutorService;
import service.UserService;
import util.CustomException;
import util.Helper;

public class LoginController {

	private final CacheUtil cacheUtil = new CacheUtil();

	public void handlePost(HttpServletRequest request, HttpServletResponse response) throws IOException {

		JsonObject jsonObject = Helper.parseRequestBody(request);
		Map<String, Object> loginMap = Helper.mapJsonObject(jsonObject);

		String username = (String) loginMap.get("username");
		String password = (String) loginMap.get("password");

		Map<String, Object> userDetails = new HashMap<>();
		try {
			UserService userService = new UserService();
			userDetails = userService.userLogin(username, password);
		} catch (CustomException e) {
			Helper.sendErrorResponse(response, e);
		}

		String jwtToken = Helper.generateJwtToken(userDetails);
		Long userId = (Long) userDetails.get("id");

		cacheUtil.saveWithTTL(userId.toString(), jwtToken, 3600);

		Map<String, Object> responseData = Helper.prepareResponseData(userDetails, jwtToken);

		Helper.sendSuccessResponse(response, responseData);

	}

	public void handleDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {

		String authHeader = request.getHeader("Authorization");

		Long userId = (Long) Helper.getThreadLocalValue("id");

		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			cacheUtil.delete(userId.toString());
			ActivityLog activityLog = new ActivityLog().setLogMessage("Logout").setLogType(LogType.Logout)
					.setUserAccountNumber(null).setRowId(userId).setTableName("User").setUserId(userId);

			TaskExecutorService.getInstance().submit(activityLog);
			Helper.sendSuccessResponse(response, "Logout successful");
		} else {
			Helper.sendErrorResponse(response, "Missing or invalid Authorization header");
		}
	}

}
