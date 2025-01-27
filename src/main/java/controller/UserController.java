package controller;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import service.UserService;
import util.CustomException;
import util.Helper;

public class UserController {

	private final UserService userService = new UserService();
	private final Logger logger = LogManager.getLogger(UserController.class);

	public void handleGet(HttpServletRequest request, HttpServletResponse response, Map<String, Object> userMap)
			throws IOException {
		logger.info("Received GET request to fetch user details. Parameters: {}", userMap);

		try {
			Object userDetails = userService.getUserDetails(userMap);
			Helper.sendSuccessResponse(response, userDetails);
			logger.info("Successfully fetched user details.");
		} catch (CustomException exception) {
			logger.error("CustomException occurred while fetching user details: {}", exception.getMessage());
			Helper.sendErrorResponse(response, exception.getMessage());
		} catch (Exception exception) {
			logger.error("Unexpected error occurred while fetching user details: {}", exception.getMessage());
			Helper.sendErrorResponse(response, "Unexpected error occurred while fetching user.");
		}
	}

	public void handleGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		logger.info("Received GET request without parameters, fetching from request.");
		handleGet(request, response, Helper.getParametersAsMap(request));
	}

	public void handlePost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		logger.info("Received POST request to create or get a user.");
		try {
			JsonObject jsonObject = Helper.parseRequestBody(request);
			Map<String, Object> userMap = Helper.mapJsonObject(jsonObject);

			if (userMap.containsKey("get")) {
				logger.info("User is requesting to fetch data. Forwarding to GET handler.");
				handleGet(request, response, userMap);
				return;
			}

			logger.info("Creating a new user with provided details.");
			userService.createUser(userMap);
			Helper.sendSuccessResponse(response, "success");
			logger.info("User created successfully.");
		} catch (CustomException exception) {
			logger.error("CustomException occurred while creating user: {}", exception.getMessage());
			Helper.sendErrorResponse(response, exception.getMessage());
		} catch (Exception exception) {
			logger.error("Unexpected error occurred while creating user: {}", exception.getMessage());
			Helper.sendErrorResponse(response, "Unexpected error occurred while creating user.");
		}
	}

	@SuppressWarnings("unchecked")
	public void handlePut(HttpServletRequest request, HttpServletResponse response) throws IOException {
		logger.info("Received PUT request to update user details.");
		try {
			JsonObject jsonObject = Helper.parseRequestBody(request);
			Gson gson = new Gson();
			Map<String, Object> userMap = gson.fromJson(jsonObject, Map.class);
			logger.info("Updating user details for: {}", userMap);
			userService.updateUserDetails(userMap);
			Helper.sendSuccessResponse(response, "success");
			logger.info("User details updated successfully.");
		} catch (CustomException exception) {
			logger.error("CustomException occurred while updating user details: {}", exception.getMessage());
			Helper.sendErrorResponse(response, exception.getMessage());
		} catch (Exception exception) {
			logger.error("Unexpected error occurred while updating user details: {}", exception);
			Helper.sendErrorResponse(response, "Unexpected error occurred while updating user.");
		}
	}

}
