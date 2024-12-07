package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import service.BranchService;
import util.CustomException;
import util.Helper;

public class BranchController {
	
	public void handlePost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		JsonObject responseJson = new JsonObject();
		BranchService branchService = new BranchService();
		try (BufferedReader reader = request.getReader()) {
			JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
			Map<String, Object> branchMap = Helper.mapJsonObject(jsonObject);
			branchService.createBranch(branchMap);
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
