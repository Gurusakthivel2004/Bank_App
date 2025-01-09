package controller;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import service.UserService;

import util.CustomException;
import util.Helper;

public class UserController {

	private final UserService userService = new UserService();

	public void handleGet(HttpServletRequest request, HttpServletResponse response, Map<String, Object> userMap)
			throws IOException {
		PrintWriter out = response.getWriter();
		response.setContentType("application/json");

		try {
			boolean notExact = userMap != null && userMap.containsKey("notExact");

			Object userDetails = userService.getUserDetails(userMap, notExact);
			ObjectMapper mapper = new ObjectMapper();
			String jsonResponse = mapper.writeValueAsString(userDetails);

			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().write(jsonResponse);
		} catch (CustomException e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			out.println(e.getMessage());
		} finally {
			out.close();
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

			System.out.println(userMap.keySet());
			System.out.println(userMap.values());

			userService.updateUserDetails(userMap);
			Helper.sendSuccessResponse(response, "success");
		} catch (CustomException exception) {
			Helper.sendErrorResponse(response, exception.getMessage());
		}
	}

}
