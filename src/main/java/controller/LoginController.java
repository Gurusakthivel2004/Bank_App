package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cache.CacheUtil;
import service.UserService;
import util.CustomException;
import util.Helper;

public class LoginController {

	private final CacheUtil cacheUtil = new CacheUtil();

	public void handlePost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("application/json");
		JsonObject responseJson = new JsonObject();

		try (BufferedReader reader = request.getReader(); PrintWriter out = response.getWriter()) {

			JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
			String username = jsonObject.get("username").getAsString();
			String password = jsonObject.get("password").getAsString();

			Map<String, Object> userDetails;
			try {
				UserService userService = new UserService();
				userDetails = userService.userLogin(username, password);
			} catch (CustomException e) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				responseJson.addProperty("message", e.getMessage());
				out.print(responseJson.toString());
				return;
			}

			String jwtToken = Helper.generateJwtToken(userDetails);
			Long userId = (Long) userDetails.get("id");

			cacheUtil.saveWithTTL(userId.toString(), jwtToken, 3600);

			Map<String, Object> responseData = Helper.prepareResponseData(userDetails, jwtToken);

			responseJson = new Gson().toJsonTree(responseData).getAsJsonObject();
			response.setStatus(HttpServletResponse.SC_OK);
			out.print(responseJson.toString());
		}
	}

	public void handleDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();

		String authHeader = request.getHeader("Authorization");
		JsonObject responseJson = new JsonObject();

		Long userId = (Long) Helper.getThreadLocalValue("id");
		System.out.println(userId);
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			System.out.println(userId);
			cacheUtil.delete(userId.toString());
			response.setStatus(HttpServletResponse.SC_OK);
			responseJson.addProperty("message", "Logout successful");
		} else {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			responseJson.addProperty("message", "Missing or invalid Authorization header");
		}
		out.print(responseJson.toString());
		out.close();
	}

}
