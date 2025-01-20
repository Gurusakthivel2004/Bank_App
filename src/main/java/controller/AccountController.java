package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import Enum.Constants.HttpStatusCodes;
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
			Helper.sendSuccessResponse(response, accounts);
		} catch (CustomException e) {
			Helper.sendErrorResponse(response, e);
		} catch (Exception e) {
			Helper.sendErrorResponse(response, "Unexpected Error occured.");
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
			Map<String, Object> accountMap = Helper.mapJsonObject(jsonObject);
			if (accountMap.containsKey("get")) {
				handleGet(request, response, accountMap);
				return;
			}
			accountService.createAccount(accountMap);
			responseJson.addProperty("message", "success");
			response.setStatus(HttpStatusCodes.OK.getCode());
		} catch (CustomException exception) {
			responseJson.addProperty("message", exception.getMessage());
			response.setStatus(HttpStatusCodes.BAD_REQUEST.getCode());
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
			Map<String, Object> accMap = new HashMap<>();
			if (jsonObject.has("branchId")) {
				branchId = jsonObject.get("branchId").getAsString();
				accMap.put("branchId", branchId);
				accountService.updateAccount(accountNumber, accMap);
			} else if (jsonObject.has("status")) {
				status = jsonObject.get("status").getAsString();
				accMap.put("status", status);
				accountService.updateAccount(accountNumber, accMap);
			}
			responseJson.addProperty("message", "success");
			response.setStatus(HttpStatusCodes.OK.getCode());
		} catch (CustomException e) {
			responseJson.addProperty("message", e.getMessage());
			response.setStatus(HttpStatusCodes.BAD_REQUEST.getCode());
		} finally {
			out.print(responseJson.toString());
			out.close();
		}
	}
}
