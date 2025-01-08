package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		JsonObject responseJson = new JsonObject();

		try (BufferedReader reader = request.getReader()) {
			JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
			Map<String, Object> userMap = Helper.mapJsonObject(jsonObject);
			if (userMap.containsKey("get")) {
				handleGet(request, response, userMap);
				return;
			}
			userService.createUser(userMap);
			responseJson.addProperty("message", "success");
			response.setStatus(HttpServletResponse.SC_OK);
		} catch (CustomException exception) {
			responseJson.addProperty("message", exception.getMessage());
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} finally {
			out.print(responseJson.toString());
			out.close();
		}
	}

	@SuppressWarnings("unchecked")
	public void handlePut(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		JsonObject responseJson = new JsonObject();

		try (BufferedReader reader = request.getReader()) {
			JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
			Gson gson = new Gson();
			Map<String, Object> userMap = gson.fromJson(jsonObject, Map.class);

			System.out.println(userMap.keySet());
			System.out.println(userMap.values());
			userService.updateUserDetails(userMap);
			responseJson.addProperty("message", "success");
			response.setStatus(HttpServletResponse.SC_OK);
		} catch (CustomException exception) {
			responseJson.addProperty("message", exception.getMessage());
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} finally {
			out.print(responseJson.toString());
			out.close();
		}
	}
}
