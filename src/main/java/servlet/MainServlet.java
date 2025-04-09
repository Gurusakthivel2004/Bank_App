package servlet;

import java.io.IOException;
import java.lang.reflect.Method;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import util.CustomException;
import util.Helper;

@SuppressWarnings("serial")

public class MainServlet extends HttpServlet {

	private static Logger logger = LogManager.getLogger(MainServlet.class);

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String origin = request.getHeader("Origin");
		if (origin != null && !origin.isEmpty()) {
			response.setHeader("Access-Control-Allow-Origin", origin);
			response.setHeader("Access-Control-Allow-Credentials", "true");
		}
		response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
		response.setHeader("Access-Control-Allow-Headers", "Origin, Content-Type, Accept, Authorization");
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			response.setStatus(HttpServletResponse.SC_OK);
			return;
		}
		try {
			logger.info("Processing request: {} {}", request.getMethod(), request.getRequestURI());
			routeRequest(request, response);
		} catch (Exception e) {
			logger.error("General error during request processing: ", e);
			Helper.handleErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
		}
	}

	private void routeRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String handler = Helper.getHandler(request);

		if (handler.equals("Logout")) {
			handler = "login";
		}

		String handlerName = Helper.capitalizeFirstLetter(handler);
		if (handlerName != null) {
			String controllerClassName = "controller." + handlerName + "Controller";
			try {
				logger.info("Routing to controller: {}", controllerClassName);
				Class<?> controllerClass = Class.forName(controllerClassName);

				Method getInstanceMethod = controllerClass.getMethod("getInstance");
				Object controllerInstance = getInstanceMethod.invoke(null);

				String methodName = "handle" + Helper.capitalizeFirstLetter(request.getMethod().toLowerCase());
				Method method = controllerClass.getMethod(methodName, HttpServletRequest.class,
						HttpServletResponse.class);
				method.invoke(controllerInstance, request, response);

			} catch (Exception e) {
				logger.error("Error processing " + handlerName + " request", e);
				Throwable cause = e.getCause();
				if (cause instanceof CustomException) {
					Helper.sendErrorResponse(response, cause.getMessage(), ((CustomException) cause).getStatusCode());
				} else {
					Helper.sendErrorResponse(response, "Error processing " + handlerName + " request.", 400);
				}
			}
		} else {
			logger.warn("No valid handler found for path: {}", request.getRequestURI());
			Helper.handleErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST,
					"No valid handler found for path.");
		}
	}

}
