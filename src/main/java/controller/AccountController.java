package controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonObject;

import service.AccountService;
import util.CustomException;
import util.Helper;

public class AccountController {

	AccountService accountService = new AccountService();

	public void handleGet(HttpServletRequest request, HttpServletResponse response, Map<String, Object> accountMap)
			throws IOException {
		try {
			Object accounts = accountService.getAccountDetails(accountMap);
			Helper.sendSuccessResponse(response, accounts);
		} catch (CustomException e) {
			Helper.sendErrorResponse(response, e);
		} catch (Exception e) {
			Helper.sendErrorResponse(response, "Unexpected Error occured.");
		}
	}

	public void handleGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		handleGet(request, response, Helper.getParametersAsMap(request));
	}

	public void handlePost(HttpServletRequest request, HttpServletResponse response) throws IOException {

		try {
			JsonObject jsonObject = Helper.parseRequestBody(request);
			Map<String, Object> accountMap = Helper.mapJsonObject(jsonObject);

			if (accountMap.containsKey("get")) {
				handleGet(request, response, accountMap);
				return;
			}

			accountService.createAccount(accountMap);
			Helper.sendSuccessResponse(response, "success");
		} catch (CustomException exception) {
			Helper.sendErrorResponse(response, exception);
		}
	}

	public void handlePut(HttpServletRequest request, HttpServletResponse response)
			throws IOException, CustomException {

		try {
			JsonObject jsonObject = Helper.parseRequestBody(request);
			Map<String, Object> accountMap = Helper.mapJsonObject(jsonObject);

			String branchId = null, status = null;
			Long accountNumber = Long.parseLong((String) accountMap.get("accountNumber"));

			Map<String, Object> accMap = new HashMap<>();
			if (accountMap.containsKey("branchId")) {
				branchId = (String) accountMap.get("branchId");
				accMap.put("branchId", branchId);
				accountService.updateAccount(accountNumber, accMap);
			} else if (accountMap.containsKey("status")) {
				status = (String) accountMap.get("status");
				accMap.put("status", status);
				accountService.updateAccount(accountNumber, accMap);
			}
			Helper.sendSuccessResponse(response, "success");
		} catch (CustomException exception) {
			Helper.sendErrorResponse(response, exception);
		}
	}
}
