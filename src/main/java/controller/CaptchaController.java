package controller;

import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class CaptchaController {

	private CaptchaController() {}

	private static class SingletonHelper {
		private static final CaptchaController INSTANCE = new CaptchaController();
	}

	public static CaptchaController getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public void handleGet(HttpServletRequest request, HttpServletResponse response) throws Exception {
		char[] chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
		Random random = new Random();
		StringBuilder captchaText = new StringBuilder();

		for (int i = 0; i < 6; i++) {
			captchaText.append(chars[random.nextInt(chars.length)]);
		}

		HttpSession session = request.getSession();
		session.setAttribute("captcha", captchaText.toString());
		session.setAttribute("captchaTimestamp", System.currentTimeMillis()); 

		response.setContentType("text/plain");
		response.getWriter().write(captchaText.toString());
	}

	public void handlePost(HttpServletRequest request, HttpServletResponse response) throws Exception {
		HttpSession session = request.getSession();
		String sessionCaptcha = (String) session.getAttribute("captcha");
		Long captchaTimestamp = (Long) session.getAttribute("captchaTimestamp");
		String userCaptcha = request.getParameter("captcha");

		// Expire CAPTCHA after 5 minutes
		if (captchaTimestamp != null && (System.currentTimeMillis() - captchaTimestamp) > 300_000) {
			session.removeAttribute("captcha");
			session.removeAttribute("captchaTimestamp");
			response.getWriter().write("expired");
			return;
		}

		if (userCaptcha == null || !userCaptcha.equalsIgnoreCase(sessionCaptcha)) {
			response.getWriter().write("fail");
		} else {
			session.removeAttribute("captcha");  
			session.removeAttribute("captchaTimestamp");
			session.setAttribute("captchaVerified", true);
			response.getWriter().write("success");
		}
	}
}
