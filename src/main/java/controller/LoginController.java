package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
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

			cacheUtil.deleteAll();

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

		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			String token = authHeader.substring(7);
			// Store blacklisted token in cache
			Map<String, String> blacklistEntry = new HashMap<>();
			blacklistEntry.put(token, "blacklisted");
			cacheUtil.saveWithTTL("blacklist", blacklistEntry, 3600);
			cacheUtil.deleteAll();
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
