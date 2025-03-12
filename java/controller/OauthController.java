package controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import enums.Constants.Role;
import io.github.cdimascio.dotenv.Dotenv;
import model.OauthProvider;
import util.CustomException;
import util.Helper;
import util.OAuthConfig;

public class OauthController {

	private static Logger logger = LogManager.getLogger(OauthController.class);

	private OauthController() {
		logger.debug("Initializing OauthController");
	}

	private static class SingletonHelper {
		private static final OauthController INSTANCE = new OauthController();
	}

	public static OauthController getInstance() {
		logger.debug("Getting instance of OauthController");
		return SingletonHelper.INSTANCE;
	}

	public void handleGet(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String provider = request.getParameter("provider");
		handleGet(request, response, provider);
	}

	public void handleGet(HttpServletRequest request, HttpServletResponse response, String provider) throws Exception {

		if (provider == null || provider.isEmpty()) {
			logger.error("Missing provider parameter in request");
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing provider parameter");
			return;
		}

		logger.info("Provider parameter received: {}", provider);

		String token = (String) request.getAttribute("token");

		if (token != null) {
			logger.info("Updating access token: {}", token);
			try {
				OauthCallbackController oauthCallbackController = OauthCallbackController.getInstance();
				String accessToken = oauthCallbackController.isValidOAuthToken(response, token);
				logger.info("Valid OAuth token obtained: {}", accessToken);

				OauthProvider oauthProvider = oauthCallbackController.getOuathProvider(accessToken);
				Map<String, Object> claimsMap = Helper.getClaimsFromId(oauthProvider.getUserId());
				logger.info("Access token generated successfully for user ID: {}", oauthProvider.getUserId());

				Role userRole = Role.fromString((String) claimsMap.get("role"));
				logger.info("User role from claims: {}", userRole);

				if (userRole == Role.Customer) {
					logger.info("Redirecting to customer dashboard.");
					response.sendRedirect("http://localhost:8080/Bank_Application/dashboard.html");
				} else {
					logger.info("Redirecting to employee dashboard.");
					response.sendRedirect("http://localhost:8080/Bank_Application/emp-dashboard.html");
				}
				return;
			} catch (IOException e) {
				logger.error("Error occurred during OAuth token validation or redirection", e);
				return;
			} catch (CustomException e) {
				logger.warn("CustomException caught - User needs to sign in again.", e);
			}
		}

		logger.info("No token found, initiating OAuth authorization process.");

		Dotenv dotenv = Helper.loadDotEnv();
		String providerCap = provider.toUpperCase();

		String authUrl = OAuthConfig.get(provider + ".auth_url");
		String clientId = dotenv.get(providerCap + "_CLIENT_ID");
		String redirectUri = OAuthConfig.get(provider + ".redirect_uri");
		String scope = OAuthConfig.get(provider + ".scope");

		if (authUrl == null || clientId == null || redirectUri == null) {
			logger.error("Invalid provider configuration: missing essential details");
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid provider configuration");
			return;
		}

		logger.info("Authorization URL: {}", authUrl);
		logger.info("OAuth client ID for provider {}: {}", provider, clientId);
		logger.info("Redirect URI: {}", redirectUri);

		try {
			String state = URLEncoder.encode(provider, "UTF-8");
			String authorizationUrl = authUrl + "?client_id=" + URLEncoder.encode(clientId, "UTF-8") + "&redirect_uri="
					+ URLEncoder.encode(redirectUri, "UTF-8") + "&response_type=code" + "&scope="
					+ URLEncoder.encode(scope, "UTF-8") + "&access_type=offline" + "&prompt=consent" + "&state="
					+ state;

			logger.info("Redirecting to OAuth authorization URL: {}", authorizationUrl);
			response.sendRedirect(authorizationUrl);
		} catch (Exception e) {
			logger.error("Error encoding or sending OAuth authorization request", e);
		}

	}
}
