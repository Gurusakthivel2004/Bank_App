package controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import service.UserService;
import util.Helper;

public class UserController {

	private UserService userService = UserService.getInstance();
	private static Logger logger = LogManager.getLogger(UserController.class);

	private UserController() {}

	private static class SingletonHelper {
		private static final UserController INSTANCE = new UserController();
	}

	public static UserController getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public void handleGet(HttpServletRequest request, HttpServletResponse response, Map<String, Object> userMap)
			throws Exception {
		logger.info("Received GET request to fetch user details. Parameters: {}", userMap);

		Object userDetails = userService.getUserDetails(userMap);
		Helper.sendSuccessResponse(response, userDetails);
		logger.info("Successfully fetched user details.");
	}

	public void handleGet(HttpServletRequest request, HttpServletResponse response) throws Exception {
		logger.info("Received GET request without parameters, fetching from request.");
		handleGet(request, response, Helper.getParametersAsMap(request));
	}
	
	private void sendUserIdResponse(Long userId, HttpServletResponse response) throws IOException {
		JsonObject responseJson = new JsonObject();
		responseJson.addProperty("message", "success");
		responseJson.addProperty("userId", userId.toString());
		response.setStatus(HttpServletResponse.SC_OK);

		try (PrintWriter out = response.getWriter()) {
			out.print(responseJson.toString());
		}
	}

	public void handlePost(HttpServletRequest request, HttpServletResponse response) throws Exception {
		logger.info("Received POST request to create or get a user.");

		JsonObject jsonObject = Helper.parseRequestBody(request);
		Map<String, Object> userMap = Helper.mapJsonObject(jsonObject);

		if (userMap.containsKey("get")) {
			logger.info("User is requesting to fetch data. Forwarding to GET handler.");
			handleGet(request, response, userMap);
			return;
		}

		logger.info("Creating a new user with provided details.");
		Long userId = userService.createUser(userMap);
		sendUserIdResponse(userId, response);
		logger.info("User created successfully.");
	}

	@SuppressWarnings("unchecked")
	public void handlePut(HttpServletRequest request, HttpServletResponse response) throws Exception {
		logger.info("Received PUT request to update user details.");

		JsonObject jsonObject = Helper.parseRequestBody(request);
		Gson gson = new Gson();
		Map<String, Object> userMap = gson.fromJson(jsonObject, Map.class);
		logger.info("Updating user details for: {}", userMap);
		userService.updateUserDetails(userMap);
		Helper.sendSuccessResponse(response, "success");
		logger.info("User details updated successfully.");
	}

}
