package controller;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonObject;

import service.AccountService;
import util.CustomException;
import util.Helper;

public class LogController {

	AccountService accountService = new AccountService();

	public void handleGet(HttpServletRequest request, HttpServletResponse response, Map<String, Object> logMap)
			throws IOException {
		try {
			Object accounts = accountService.getAccountDetails(logMap);
			Helper.sendSuccessResponse(response, accounts);
		} catch (CustomException e) {
			Helper.sendErrorResponse(response, e);
		} catch (Exception e) {
			Helper.sendErrorResponse(response, "Unexpected Error occured.");
		}
	}

	public void handlePost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		JsonObject jsonObject = Helper.parseRequestBody(request);
		Map<String, Object> logMap = Helper.mapJsonObject(jsonObject);
		handleGet(request, response, logMap);
	}

}
