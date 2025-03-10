package service;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class NotificationService {

	private static final Logger logger = Logger.getLogger(NotificationService.class.getName());

	private static final String SMTP_HOST = "smtp.gmail.com";
	private static final String SMTP_PORT = "587";
	private static final String SMTP_USERNAME = "vijayguru2004@gmail.com";
	private static final String SMTP_PASSWORD = "aymd zeuz xwih jiqo";

	public void sendEmail(String toEmail, String subject, String messageBody) {
		logger.info("Initializing email sending process...");

		Properties properties = new Properties();
		properties.put("mail.smtp.auth", "true");
		properties.put("mail.smtp.starttls.enable", "true");
		properties.put("mail.smtp.host", SMTP_HOST);
		properties.put("mail.smtp.port", SMTP_PORT);
		properties.put("mail.smtp.ssl.protocols", "TLSv1.2");
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

		try {
			logger.info("Preparing email message...");
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(SMTP_USERNAME));
			message.setRecipients(MimeMessage.RecipientType.TO, InternetAddress.parse(toEmail));
			message.setSubject(subject);
			message.setText(messageBody);

			logger.info("Message created successfully.");
			Transport transport = session.getTransport("smtp");
			transport.connect(SMTP_HOST, SMTP_USERNAME, SMTP_PASSWORD);
			transport.sendMessage(message, message.getAllRecipients());
			transport.close();

			logger.info("Email sent successfully!");
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error sending email!", e);
		}

	}
}