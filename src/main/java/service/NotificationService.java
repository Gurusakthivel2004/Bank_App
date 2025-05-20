package service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dao.DAO;
import dao.DaoFactory;
import enums.Constants.TaskExecutor;
import io.github.cdimascio.dotenv.Dotenv;
import model.Criteria;
import model.OtpVerifications;
import model.User;
import util.Helper;
import util.SQLHelper;

public class NotificationService {

	private static final Logger logger = LogManager.getLogger(NotificationService.class);
	private DAO<OtpVerifications> otpVerificationsDAO = DaoFactory.getDAO(OtpVerifications.class);

	private static Dotenv dotenv = Helper.loadDotEnv();
	private static final String SMTP_HOST = dotenv.get("SMTP_HOST");
	private static final String SMTP_PORT = dotenv.get("SMTP_PORT");
	private static final String SMTP_USERNAME = dotenv.get("SMTP_USERNAME");
	private static final String SMTP_PASSWORD = dotenv.get("SMTP_PASSWORD");

	private NotificationService() {
	}

	private static class SingletonHelper {
		private static final NotificationService INSTANCE = new NotificationService();
	}

	public static NotificationService getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public void sendEmailAysnc(String toEmail, String subject, String messageBody) {
		logger.info("Initializing email sending process...");

		Properties properties = new Properties();
		properties.put("mail.smtp.auth", "true");
		properties.put("mail.smtp.starttls.enable", "true");
		properties.put("mail.smtp.starttls.required", "true");
		properties.put("mail.smtp.ssl.protocols", "TLSv1.2");

		properties.put("mail.smtp.host", SMTP_HOST);
		properties.put("mail.smtp.port", SMTP_PORT);
		properties.put("mail.debug", "true");

		Session session = Session.getInstance(properties, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				logger.info("Authenticating SMTP credentials...");
				return new PasswordAuthentication(SMTP_USERNAME, SMTP_PASSWORD);
			}
		});
		session.setProtocolForAddress("rfc822", "smtp");
		session.setDebug(true);
		session.setDebugOut(System.out);

		try {
			logger.info("Preparing email message...");
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(SMTP_USERNAME));
			message.setRecipients(MimeMessage.RecipientType.TO, InternetAddress.parse(toEmail));
			message.setSubject(subject);
			message.setText(messageBody);

			Transport transport = session.getTransport("smtp");
			transport.connect(SMTP_HOST, SMTP_USERNAME, SMTP_PASSWORD);
			transport.sendMessage(message, message.getAllRecipients());
			transport.close();

			logger.info("Email sent successfully!");
		} catch (Exception e) {
			logger.error("Error sending email!", e);
		}
	}

	public void sendEmail(String toEmail, String subject, String messageBody) {
		TaskExecutor.MAIL.submitTask(() -> {
			sendEmailAysnc(toEmail, subject, messageBody);
		});
	}

	public void sendOtp(Long userId, Long accountNumber, String email) throws Exception {
		User user = UserService.getInstance().getUserById(userId);
		String otp = Helper.generateOTP();
		String userEmail = email != null ? email : user.getEmail();
		sendEmail(userEmail, "One-time Password", otp + " is your otp to process the payment.");

		deleteOtp(accountNumber != null ? accountNumber : userId);

		OtpVerifications otpVerifications = new OtpVerifications();
		otpVerifications.setUserId(userId);
		otpVerifications.setAccountNumber(accountNumber);
		otpVerifications.setIsVerified(false);
		otpVerifications.setAttempts(0);
		otpVerifications.setOtp(otp);
		otpVerifications.setCreatedAt(System.currentTimeMillis());
		// expires in 5 min
		otpVerifications.setExpiresAt(System.currentTimeMillis() + (5 * 60 * 1000));
		otpVerificationsDAO.create(otpVerifications);
	}

	public void deleteOtp(long accountNumber) throws Exception {
		Criteria criteria = new Criteria().setClazz(OtpVerifications.class);
		criteria.setColumn(new ArrayList<String>(Arrays.asList("account_number")));
		criteria.setOperator(new ArrayList<String>(Arrays.asList("EQUAL_TO")));
		criteria.setValue(new ArrayList<Object>(Arrays.asList(accountNumber)));
		SQLHelper.delete(criteria);
	}

}