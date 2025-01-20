package controller;

import java.io.IOException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.gson.JsonObject;
import service.TransactionService;
import util.CustomException;
import util.Helper;

public class TransactionController {

	TransactionService transactionService = new TransactionService();

	public void handleGet(HttpServletRequest request, HttpServletResponse response, Map<String, Object> txMap)
			throws IOException {
		try {
			Object transactions = transactionService.getTransactionDetails(txMap);
			Helper.sendSuccessResponse(response, transactions);
		} catch (CustomException exception) {
			Helper.sendErrorResponse(response, exception);
		}
	}

	public void handlePost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		JsonObject jsonObject = Helper.parseRequestBody(request);
		Map<String, Object> transactionMap = Helper.mapJsonObject(jsonObject);

		if (transactionMap.containsKey("get")) {
			handleGet(request, response, transactionMap);
			return;
		}
		try {
			transactionService.prepareTransaction(transactionMap);
			Helper.sendSuccessResponse(response, "success");
		} catch (CustomException exception) {
			Helper.sendErrorResponse(response, exception);
		}
	}

}
