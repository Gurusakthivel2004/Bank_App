package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import service.UserService;
import util.CustomException;

public class ProfileController {
	
	public void handlePut(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		JsonObject responseJson = new JsonObject();

		try (BufferedReader reader = request.getReader()) {
			JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
			String currentPassword = jsonObject.get("currentPassword").getAsString();
			String newPassword = jsonObject.get("newPassword").getAsString();
			UserService userService = new UserService();
			boolean result = userService.updatePassword(currentPassword, newPassword);
			// response Json
			responseJson.addProperty("success", result);
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