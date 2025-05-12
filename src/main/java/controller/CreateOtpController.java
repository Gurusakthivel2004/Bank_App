package controller;

import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

import service.NotificationService;
import util.Helper;

public class CreateOtpController {
	
	private static final Logger LOGGER = LogManager.getLogger(OtpController.class);

	private CreateOtpController() {}

	private static class SingletonHelper {
		private static final CreateOtpController INSTANCE = new CreateOtpController();
	}

	public static CreateOtpController getInstance() {
		return SingletonHelper.INSTANCE;
	}
	
	public void handlePost(HttpServletRequest request, HttpServletResponse response) throws Exception {
		LOGGER.info("Creating and sending OTP to the user..");
		Long userId = (Long) Helper.getThreadLocalValue("id");
		HttpSession session = request.getSession();
		
		Random random = new Random();
		long accountNumber = Math.abs(random.nextLong());
		session.setAttribute("accountNumber", accountNumber);
		
		String email = extractFromRequest(request, "email");
		
		NotificationService.getInstance().sendOtp(userId, accountNumber, email);
		Helper.sendSuccessResponse(response, "success");
	}
	
	private String extractFromRequest(HttpServletRequest request, String key) throws Exception {
		JsonObject jsonObject = Helper.parseRequestBody(request);
		LOGGER.debug("Parsed request body: {}", jsonObject);

		Map<String, Object> otpMap = Helper.mapJsonObject(jsonObject);
		return otpMap.get(key).toString();
	}
}