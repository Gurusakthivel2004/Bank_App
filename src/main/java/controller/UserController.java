package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import service.UserService;
import util.CustomException;
import util.Helper;

public class UserController {
	UserService userService = new UserService();
	public void handleGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		PrintWriter out = response.getWriter();
		response.setContentType("application/json");
		try {
			long userId = (long) Helper.getThreadLocalValue().get("id");
			Object userDetails = userService.getUserDetails(userId);
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

	public void handlePost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		JsonObject responseJson = new JsonObject();

		try (BufferedReader reader = request.getReader()) {
			JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
			Map<String, Object> userMap = Helper.mapJsonObject(jsonObject);

			userService.createUser(userMap);
			responseJson.addProperty("message", "success");
			response.setStatus(HttpServletResponse.SC_OK);
		} catch (CustomException exception) {
			// Handle custom exception for failed account creation
			responseJson.addProperty("message", exception.getMessage());
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} finally {
			out.print(responseJson.toString());
			out.close();
		}
	}

	public void handlePut(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		JsonObject responseJson = new JsonObject();

		try (BufferedReader reader = request.getReader()) {
			JsonArray jsonArray = JsonParser.parseReader(reader).getAsJsonArray();
			Map<String, Object> userMap = new HashMap<>();
			for (JsonElement element : jsonArray) {
				String field = element.getAsJsonObject().get("name").getAsString();
				JsonElement elementValue = element.getAsJsonObject().get("value");
				Object value = Helper.convertJsonElement(elementValue);
				userMap.put(field, value);
			}
			userService.updateUserDetails(userMap);
			responseJson.addProperty("message", "success");
			response.setStatus(HttpServletResponse.SC_OK);
		} catch (CustomException exception) {
			// Handle custom exception for failed account creation
			responseJson.addProperty("message", exception.getMessage());
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} finally {
			out.print(responseJson.toString());
			out.close();
		}
	}
}
