package controller;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import service.UserService;
import util.CustomException;
import util.Helper;

public class UserController {

	private final UserService userService = new UserService();

	public void handleGet(HttpServletRequest request, HttpServletResponse response, Map<String, Object> userMap)
			throws IOException {

		try {
			Object userDetails = userService.getUserDetails(userMap);
			Helper.sendSuccessResponse(response, userDetails);
		} catch (CustomException exception) {
			Helper.sendErrorResponse(response, exception.getMessage());
		}
	}

	public void handleGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		handleGet(request, response, Helper.getParametersAsMap(request));
	}

	public void handlePost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			JsonObject jsonObject = Helper.parseRequestBody(request);
			Map<String, Object> userMap = Helper.mapJsonObject(jsonObject);

			if (userMap.containsKey("get")) {
				handleGet(request, response, userMap);
				return;
			}

			userService.createUser(userMap);
			Helper.sendSuccessResponse(response, "success");
		} catch (CustomException exception) {
			Helper.sendErrorResponse(response, exception.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	public void handlePut(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			JsonObject jsonObject = Helper.parseRequestBody(request);
			Gson gson = new Gson();
			Map<String, Object> userMap = gson.fromJson(jsonObject, Map.class);
			userService.updateUserDetails(userMap);
			Helper.sendSuccessResponse(response, "success");
		} catch (CustomException exception) {
			exception.printStackTrace();
			Helper.sendErrorResponse(response, exception.getMessage());
		}
	}

}
