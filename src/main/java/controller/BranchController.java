package controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;

import Enum.Constants.HttpStatusCodes;
import service.BranchService;
import util.CustomException;
import util.Helper;

public class BranchController {

	private final BranchService branchService = new BranchService();

	public void handlePost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			JsonObject jsonObject = Helper.parseRequestBody(request);
			Map<String, Object> branchMap = Helper.mapJsonObject(jsonObject);

			branchService.createBranch(branchMap);
			Helper.sendSuccessResponse(response, "success");
		} catch (CustomException exception) {
			Helper.sendErrorResponse(response, exception.getMessage());
		}
	}

	public void handleGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		Long branchId = Helper.parseLongOrDefault(request.getParameter("branchId"), 0L);
		String notExact = request.getParameter("notExact");
		try {
			if (branchId == 0L) {
				throw new CustomException("Invalid branch id ", HttpStatusCodes.BAD_REQUEST);
			}
			List<Object> branches = branchService.getBranchDetails(branchId, notExact != null);
			ObjectMapper mapper = new ObjectMapper();
			String jsonResponse = mapper.writeValueAsString(branches);
			out.write(jsonResponse);
		} catch (CustomException e) {
			JsonObject responseJson = new JsonObject();
			responseJson.addProperty("message", e.getMessage());
			out.write(responseJson.toString());
		} finally {
			out.close();
		}
	}

}
