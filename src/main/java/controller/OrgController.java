package controller;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

import model.Org;
import service.OrgService;
import util.Helper;

public class OrgController {

	private static Logger logger = LogManager.getLogger(OrgController.class);
	private OrgService orgService = OrgService.getInstance();

	private OrgController() {}

	private static class SingletonHelper {
		private static final OrgController INSTANCE = new OrgController();
	}

	public static OrgController getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public void handleGet(HttpServletRequest request, HttpServletResponse response) throws Exception {
		logger.info("Received GET request to fetch org details.");

		Map<String, Object> orgMap = Helper.getParametersAsMap(request);
		logger.debug("Extracted parameters from request: {}", orgMap);

		List<Org> orgData = orgService.getOrgDetails(orgMap);
		logger.info("Fetched org details successfully. Total org data found: {}", orgData.size());

		Helper.sendSuccessResponse(response, orgData);
	}

	public void handlePost(HttpServletRequest request, HttpServletResponse response) throws Exception {
		logger.info("Received POST request to create Org.");

		JsonObject jsonObject = Helper.parseRequestBody(request);
		logger.debug("Parsed request body: {}", jsonObject);

		Map<String, Object> orgMap = Helper.mapJsonObject(jsonObject);
		logger.debug("Mapped JSON to orgMap: {}", orgMap);

		orgService.createOrg(orgMap);
		logger.info("Org created successfully. Details: {}", orgMap);
		Helper.sendSuccessResponse(response, "success");
	}
}