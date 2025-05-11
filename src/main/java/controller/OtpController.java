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

	private OtpController() {}

	private static class SingletonHelper {
		private static final OtpController INSTANCE = new OtpController();
	}

	public static OtpController getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public void handleGet(HttpServletRequest request, HttpServletResponse response) throws Exception {
		Long userId = (Long) Helper.getThreadLocalValue("id");
		User user = UserService.getInstance().getUserById(userId);
		String otp = Helper.generateOTP();
		NotificationService.getInstance().sendEmail(user.getEmail(), "One-time Password",
				otp + " is your otp to process the payment.");

		ColumnCriteria columnCriteria = new ColumnCriteria().setFields(Arrays.asList("otp", "createdAt", "expiresAt"))
				.setValues(
						Arrays.asList(otp, System.currentTimeMillis(), System.currentTimeMillis() + (5 * 60 * 1000)));

		Map<String, Object> txMap = new HashMap<>();
		txMap.put("userId", userId);

		otpVerificationsDAO.update(columnCriteria, txMap);

		Helper.sendSuccessResponse(response, "success");
	}

	public void handlePost(HttpServletRequest request, HttpServletResponse response) throws Exception {
		HttpSession session = request.getSession();

		validateOtpSession(session);

		Long txId = (Long) session.getAttribute("txId");
		Long accountNumber = (Long) session.getAttribute("accountNumber");

		OtpVerifications otpVerification = fetchLatestOtp(accountNumber);

		String otp = extractFromRequest(request, "otp");
		String serviceRequired = extractFromRequest(request, "serviceRequired");

		validateOtp(otpVerification, otp);

		if(serviceRequired.equals("Transaction")) {
			completeTransaction(session, txId, accountNumber);
		}
		Helper.sendSuccessResponse(response, "success");
	}

	private void validateOtpSession(HttpSession session) throws CustomException {
		Boolean otpVerified = (Boolean) session.getAttribute("otpVerified");

		if (Boolean.TRUE.equals(otpVerified)) {
			throw new CustomException("Please use a new OTP.", HttpStatusCodes.BAD_REQUEST);
		}
	}

	private OtpVerifications fetchLatestOtp(Long accountNumber) throws Exception {
		Map<String, Object> otpCriteriaMap = new HashMap<>();
		otpCriteriaMap.put("accountNumber", accountNumber);

		List<OtpVerifications> otpList = otpVerificationsDAO.get(otpCriteriaMap);

		if (otpList == null || otpList.isEmpty()) {
			throw new CustomException("No OTP found. Please request a new one.", HttpStatusCodes.BAD_REQUEST);
		}

		return otpList.get(0);
	}

	private String extractFromRequest(HttpServletRequest request, String key) throws Exception {
		JsonObject jsonObject = Helper.parseRequestBody(request);
		logger.debug("Parsed request body: {}", jsonObject);

		Map<String, Object> otpMap = Helper.mapJsonObject(jsonObject);
		return otpMap.get(key).toString();
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

	private void completeTransaction(HttpSession session, Long txId, Long accountNumber) throws Exception {
		TransactionService.getInstance().updateTransaction(txId);
		session.removeAttribute("txId");
		session.removeAttribute("accountNumber");
		session.removeAttribute("otpVerified");

		NotificationService.getInstance().deleteOtp(accountNumber);
	}

}
