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
import service.TransactionService;
import util.CustomException;
import util.Helper;

public class TransactionController {

	TransactionService transactionService = new TransactionService();

	public void handleGet(HttpServletRequest request, HttpServletResponse response, Map<String, Object> txMap) throws IOException {
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		JsonObject responseJson = new JsonObject();
		try {
			Object transactions = transactionService.getTransactionDetails(txMap);
			String jsonResponse = new ObjectMapper().writeValueAsString(transactions);
			out.write(jsonResponse);
		} catch (CustomException exception) {
			responseJson.addProperty("message", exception.getMessage());
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			out.print(responseJson.toString());
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
			Map<String, Object> transactionMap = Helper.mapJsonObject(jsonObject);
			if(transactionMap.containsKey("get")) {
				handleGet(request, response, transactionMap);
				return;
			}
			transactionService.prepareTransaction(transactionMap);
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
}
