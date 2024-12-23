package controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import service.FacadeHandler;
import util.CustomException;

public class UserDashboardController {
	public void handleGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		PrintWriter out = response.getWriter();
		response.setContentType("application/json");
		try {
			FacadeHandler facadeHandler = new FacadeHandler();
			Map<String, Object> data = facadeHandler.dashBoardDetails();
			System.out.println(data);
			ObjectMapper mapper = new ObjectMapper();
			String jsonResponse = mapper.writeValueAsString(data);
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().write(jsonResponse);
		} catch (CustomException e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			out.println(e.getMessage());
		} finally {
			out.close();
		}
	}
}