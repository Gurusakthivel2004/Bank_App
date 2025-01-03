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
import service.CacheService;
import service.UserService;
import util.CustomException;
import util.JwtUtil;

public class LoginController {

	private final CacheService cacheService = new CacheService();

	public void handlePost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("application/json");
		JsonObject responseJson = new JsonObject();
		PrintWriter out = response.getWriter();
		try (BufferedReader reader = request.getReader()) {
			// Parse the request body
			JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
			String username = jsonObject.get("username").getAsString();
			String password = jsonObject.get("password").getAsString();

			cacheService.deleteAll();
			// Authenticate user
			UserService userService = new UserService();
			Map<String, Object> userDetails = userService.userLogin(username, password);

			// Prepare JWT claims dynamically
			Map<String, Object> jwtClaims = new HashMap<>();
			jwtClaims.put("id", userDetails.get("id"));
			jwtClaims.put("role", userDetails.get("role"));
			jwtClaims.put("username", userDetails.get("username"));
			if (userDetails.containsKey("branchId")) {
				jwtClaims.put("branchId", userDetails.get("branchId"));
			}

			String jwtToken = JwtUtil.generateToken(jwtClaims);
			Map<String, Object> responseData = new HashMap<>();
			responseData.put("token", jwtToken);
			responseData.putAll(userDetails); // Adds all user details from the map
			responseData.put("message", "success");

			// Convert map to JSON
			responseJson = new Gson().toJsonTree(responseData).getAsJsonObject();
			response.setStatus(HttpServletResponse.SC_OK);
		} catch (CustomException e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			responseJson.addProperty("message", e.getMessage());
		} finally {
			out.print(responseJson.toString());
			out.close();
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
			cacheService.saveWithTTL("blacklist", blacklistEntry, 3600);
			cacheService.deleteAll();
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
