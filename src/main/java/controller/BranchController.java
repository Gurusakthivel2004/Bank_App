package controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonObject;

import model.Branch;
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

		Map<String, Object> branchMap = Helper.getParametersAsMap(request);
		try {

			List<Branch> branches = branchService.getBranchDetails(branchMap);
			Helper.sendSuccessResponse(response, branches);

		} catch (CustomException exception) {
			Helper.sendErrorResponse(response, exception.getMessage());
		}
	}

}
