package controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

import dao.DAO;
import dao.DaoFactory;
import enums.Constants.HttpStatusCodes;
import model.ColumnCriteria;
import model.OtpVerifications;
import model.User;
import service.NotificationService;
import service.TransactionService;
import service.UserService;
import util.CustomException;
import util.Helper;

public class OtpController {

	private static Logger logger = LogManager.getLogger(OtpController.class);
	private DAO<OtpVerifications> otpVerificationsDAO = DaoFactory.getDAO(OtpVerifications.class);

	private OtpController() {
	}

	private static class SingletonHelper {
		private static final OtpController INSTANCE = new OtpController();
	}

	public static OtpController getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public void handleGet(HttpServletRequest request, HttpServletResponse response) throws Exception {
		logger.info("Received GET request to fetch to resend OTP.");

		Map<String, Object> otpMap = Helper.getParametersAsMap(request);
		logger.debug("Extracted parameters from request: {}", otpMap);
		HttpSession session = request.getSession();

		String email = null;
		Long userId = (Long) Helper.getThreadLocalValue("id");
		Long accountNumber = (Long) session.getAttribute("accountNumber");
		
		if (otpMap.containsKey("email")) {
			email = (String) otpMap.get("email");
		} else {
			User user = UserService.getInstance().getUserById(userId);
			email = user.getEmail();
		}
		
		String otp = Helper.generateOTP();
		NotificationService.getInstance().sendEmail(email, "One-time Password",
				otp + " is your otp to process the payment.");

		ColumnCriteria columnCriteria = new ColumnCriteria().setFields(Arrays.asList("otp", "createdAt", "expiresAt"))
				.setValues(
						Arrays.asList(otp, System.currentTimeMillis(), System.currentTimeMillis() + (5 * 60 * 1000)));

		Map<String, Object> txMap = new HashMap<>();
		txMap.put("userId", userId);
		txMap.put("accountNumber", accountNumber);

		otpVerificationsDAO.update(columnCriteria, txMap);

		Helper.sendSuccessResponse(response, "success");
	}

	public void handlePost(HttpServletRequest request, HttpServletResponse response) throws Exception {
		HttpSession session = request.getSession();

		validateOtpSession(session);

		Long accountNumber = (Long) session.getAttribute("accountNumber");
		Long userId = (Long) Helper.getThreadLocalValue("id");

		OtpVerifications otpVerification = fetchLatestOtp(userId, accountNumber);

		JsonObject requestBody = Helper.parseRequestBody(request);
		logger.debug("Parsed request body: {}", requestBody);

		String otp = requestBody.get("otp").getAsString();
		String serviceRequired = null;
		if (requestBody.has("serviceRequired") && !requestBody.get("serviceRequired").isJsonNull()) {
			serviceRequired = requestBody.get("serviceRequired").getAsString();
		}

		validateOtp(otpVerification, otp);

		if (serviceRequired != null && serviceRequired.equals("Transaction")) {
			completeTransaction(session, accountNumber);
		}
		Helper.sendSuccessResponse(response, "success"); 
	}

	private void validateOtpSession(HttpSession session) throws CustomException {
		Boolean otpVerified = (Boolean) session.getAttribute("otpVerified");

		if (Boolean.TRUE.equals(otpVerified)) {
			throw new CustomException("Please use a new OTP.", HttpStatusCodes.BAD_REQUEST);
		}
	}

	private OtpVerifications fetchLatestOtp(Long userId, Long accountNumber) throws Exception {
		Map<String, Object> otpCriteriaMap = new HashMap<>();

		if (accountNumber != null) {
			otpCriteriaMap.put("accountNumber", accountNumber);
		}
		if (userId != null) {
			otpCriteriaMap.put("userId", userId);
		}

		List<OtpVerifications> otpList = otpVerificationsDAO.get(otpCriteriaMap);

		if (otpList == null || otpList.isEmpty()) {
			throw new CustomException("No OTP found. Please request a new one.", HttpStatusCodes.BAD_REQUEST);
		}

		return otpList.get(0);
	}

	private void validateOtp(OtpVerifications otpVerification, String otp) throws CustomException {
		if (!otpVerification.getOtp().equals(otp)) {
			throw new CustomException("Invalid OTP. Please try again or request a new one.", HttpStatusCodes.NOT_FOUND);
		}

		if (otpVerification.getExpiresAt() < System.currentTimeMillis()) {
			throw new CustomException("OTP expired. Please request a new one.", HttpStatusCodes.BAD_REQUEST);
		}

		if (Boolean.TRUE.equals(otpVerification.getIsVerified())) {
			throw new CustomException("OTP already used. Please request a new one.", HttpStatusCodes.BAD_REQUEST);
		}
	}

	private void completeTransaction(HttpSession session, Long accountNumber) throws Exception {
		Long txId = (Long) session.getAttribute("txId");
		TransactionService.getInstance().updateTransaction(txId);
		session.removeAttribute("txId");
		session.removeAttribute("accountNumber");
		session.removeAttribute("otpVerified");

		NotificationService.getInstance().deleteOtp(accountNumber);
	}

}
