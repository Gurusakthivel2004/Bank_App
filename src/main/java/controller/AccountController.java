package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import service.AccountService;
import util.CustomException;
import util.Helper;

public class AccountController {

	AccountService accountService = new AccountService();

	public void handleGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		Map<String, Object> accountMap = new HashMap<>();

		Long customerId = Helper.parseLongOrDefault(request.getParameter("customerId"), 0L);
		Long accountNumber = Helper.parseLongOrDefault(request.getParameter("accountNumber"), 0L);
		Long branchId = Helper.parseLongOrDefault(request.getParameter("branchId"), 0L);
		Long accountCreated = Helper.parseDateToMillisOrDefault(request.getParameter("lastAccount"), 0L);

		try {
			if (customerId == -1) {
				accountMap.put("customerId", (Long) Helper.getThreadLocalValue().get("id"));
				customerId = (Long) Helper.getThreadLocalValue().get("id");
			}
			branchId = branchId == -1 ? (Long) Helper.getThreadLocalValue().get("branchId") : branchId;
			Object accounts = accountService.getAccountDetails(customerId, accountNumber, branchId, accountCreated);
			ObjectMapper mapper = new ObjectMapper();
			String jsonResponse = mapper.writeValueAsString(accounts);
			out.write(jsonResponse);
		} catch (CustomException e) {
			JsonObject responseJson = new JsonObject();
			responseJson.addProperty("message", e.getMessage());
			out.write(responseJson.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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
			Map<String, Object> accountMap = Helper.mapJsonObject(jsonObject);
			accountService.createAccount(accountMap);

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
