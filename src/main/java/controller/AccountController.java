package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
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

	public void handleGet(HttpServletRequest request, HttpServletResponse response, Map<String, Object> accountMap)
			throws IOException {
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		try {
			Object accounts = accountService.getAccountDetails(accountMap);
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
			if (accountMap.containsKey("get")) {
				handleGet(request, response, accountMap);
				return;
			}
			accountService.createAccount(accountMap);
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

	public void handlePut(HttpServletRequest request, HttpServletResponse response)
			throws IOException, CustomException {
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		JsonObject responseJson = new JsonObject();

		try (BufferedReader reader = request.getReader()) {
			JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();

			String branchId = null, status = null;
			Long accountNumber = jsonObject.get("accountNumber").getAsLong();
			if (jsonObject.has("branchId")) {
				branchId = jsonObject.get("branchId").getAsString();
				accountService.updateAccount(accountNumber, "branchId", branchId);
			} else if (jsonObject.has("status")) {
				status = jsonObject.get("status").getAsString();
				accountService.updateAccount(accountNumber, "status", status);
			}
			responseJson.addProperty("message", "success");
			response.setStatus(HttpServletResponse.SC_OK);
		} finally {
			out.print(responseJson.toString());
			out.close();
		}
	}
}
