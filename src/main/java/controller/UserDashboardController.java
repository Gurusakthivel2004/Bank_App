package controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import service.FacadeHandler;
import util.CustomException;
import util.Helper;

public class UserDashboardController {
	public void handleGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			FacadeHandler facadeHandler = new FacadeHandler();
			Map<String, Object> data = facadeHandler.dashBoardDetails();
			Helper.sendSuccessResponse(response, data);
		} catch (CustomException e) {
			Helper.sendErrorResponse(response, e);
		}
	}
}