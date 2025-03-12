package controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cache.CacheUtil;
import dao.DAO;
import dao.OauthProviderDAO;
import enums.Constants.HttpStatusCodes;
import enums.Constants.Role;
import io.github.cdimascio.dotenv.Dotenv;
import model.ColumnCriteria;
import model.OauthProvider;
import model.User;
import service.UserService;
import util.CustomException;
import util.Helper;
import util.OAuthConfig;

public class OauthCallbackController {

	private UserService userService = UserService.getInstance();
	private DAO<OauthProvider> oauthProviderDAO = OauthProviderDAO.getInstance();
	private static Logger logger = LogManager.getLogger(OauthCallbackController.class);

	private OauthCallbackController() {
	}

	private static class SingletonHelper {
		private static final OauthCallbackController INSTANCE = new OauthCallbackController();
	}

	public static OauthCallbackController getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public void handleGet(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String provider = request.getParameter("state");
		String code = request.getParameter("code");

		if (provider == null || code == null) {
			Helper.sendJsonResponse(response, HttpStatusCodes.BAD_REQUEST, "Invalid Oauth callback.", null);
			return;
		}

		Dotenv dotenv = Helper.loadDotEnv();
		String providerCap = provider.toUpperCase();

		String tokenUrl = OAuthConfig.get(provider + ".token_url");
		String clientId = dotenv.get(providerCap + "_CLIENT_ID");
		String clientSecret = dotenv.get(providerCap + "_CLIENT_SECRET");
		String redirectUri = OAuthConfig.get(provider + ".redirect_uri");

		String params = "code=" + URLEncoder.encode(code, "UTF-8") + "&client_id="
				+ URLEncoder.encode(clientId, "UTF-8") + "&client_secret=" + URLEncoder.encode(clientSecret, "UTF-8")
				+ "&redirect_uri=" + URLEncoder.encode(redirectUri, "UTF-8") + "&grant_type=authorization_code";

		// Get Access Token
		String tokenResponse = Helper.sendPostRequest(tokenUrl, params);
		JsonObject tokenJson = JsonParser.parseString(tokenResponse).getAsJsonObject();
		String accessToken = tokenJson.get("access_token").getAsString();

		if (!tokenJson.has("access_token")) {
			Helper.sendJsonResponse(response, HttpStatusCodes.BAD_REQUEST, "Access token missing from provider.", null);
			return;
		}

		String refreshToken = tokenJson.has("refresh_token") ? tokenJson.get("refresh_token").getAsString() : null;
		int expiresIn = tokenJson.has("expires_in") ? tokenJson.get("expires_in").getAsInt() : 0;

		// Get User Info
		String userInfoUrl = OAuthConfig.get(provider + ".user_info_url");
		String userInfoResponse = Helper.sendGetRequest(userInfoUrl + "?access_token=" + accessToken);
		JsonObject userInfoJson = JsonParser.parseString(userInfoResponse).getAsJsonObject();

		// Extract User Info
		String userEmail = userInfoJson.has("email") ? userInfoJson.get("email").getAsString() : "Unknown";
		String providerUserId = userInfoJson.has("id") ? userInfoJson.get("id").getAsString()
				: userInfoJson.has("sub") ? userInfoJson.get("sub").getAsString() : null;

		// Store OAuth details
		OauthProvider oauthProvider = new OauthProvider();
		oauthProvider.setAccessToken(accessToken);
		oauthProvider.setProvider(provider);
		oauthProvider.setProviderUserId(providerUserId);
		oauthProvider.setExpiresIn(expiresIn);
		if (refreshToken != null) {
			oauthProvider.setRefreshToken(refreshToken);
		}

		// Set session and redirect
		setSessionAndRedirect(request, response, userEmail, oauthProvider);
	}

	public String isValidOAuthToken(HttpServletResponse response, String accessToken) throws Exception {

		String userInfoEndpoint = "https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=" + accessToken;
		try {
			String getresponse = Helper.sendGetRequest(userInfoEndpoint);
			return getresponse.contains("email") ? accessToken : null;
		} catch (Exception e) {
			OauthProvider oauthProvider = getOuathProvider(accessToken);
			String refreshToken = oauthProvider.getRefreshToken();

			accessToken = refreshAccessToken(refreshToken, "google");
			updateAccessToken(accessToken, refreshToken);

			Helper.setCookie(response, "token", accessToken, 604800, true);
			return accessToken;
		}
	}

	public OauthProvider getOuathProvider(String accessToken) throws Exception {
		Map<String, Object> oauthProviderMap = new HashMap<>();
		oauthProviderMap.put("accessToken", accessToken);
		List<OauthProvider> oauthProviders = oauthProviderDAO.get(oauthProviderMap);

		if (oauthProviders.isEmpty()) {
			throw new CustomException("Invalid access token", HttpStatusCodes.BAD_REQUEST);
		}

		return oauthProviders.get(0);
	}

	public String refreshAccessToken(String refreshToken, String provider) throws CustomException, IOException {
		String tokenUrl = OAuthConfig.get(provider + ".token_url");

		Dotenv dotenv = Helper.loadDotEnv();
		String providerCap = provider.toUpperCase();
		String clientId = dotenv.get(providerCap + "_CLIENT_ID");
		String clientSecret = dotenv.get(providerCap + "_CLIENT_SECRET");

		String params = "client_id=" + clientId + "&client_secret=" + clientSecret + "&refresh_token=" + refreshToken
				+ "&grant_type=refresh_token";

		String response = Helper.sendPostRequest(tokenUrl, params);

		if (response.contains("error")) {
			logger.error("Refresh token is invalid or expired. User needs to sign in again.");
			throw new CustomException("Session expired. Please sign in again.", HttpStatusCodes.UNAUTHORIZED);
		}
		JsonObject tokenJson = JsonParser.parseString(response).getAsJsonObject();
		String accessToken = tokenJson.get("access_token").getAsString();
		return accessToken;
	}

	@SuppressWarnings("unchecked")
	private void setSessionAndRedirect(HttpServletRequest request, HttpServletResponse response, String email,
			OauthProvider oauthProvider) throws Exception {
		Map<String, Object> userMap = new HashMap<>();
		userMap.put("email", email);
		userMap.put("userClass", User.class);

		userMap = userService.getUserDetails(userMap);
		List<User> users = (List<User>) userMap.get("users");
		if (users.isEmpty()) {
			throw new CustomException("No user found with the email provided", HttpStatusCodes.BAD_REQUEST);
		}
		User user = users.get(0);
		Role role = Role.fromString(user.getRole());

		oauthProvider.setUserId(user.getId());
		oauthProvider.setCreatedAt(System.currentTimeMillis());

		String sessionId = UUID.randomUUID().toString();
		createOrUpdate(oauthProvider, sessionId);
		setCookie(response, user, sessionId, userMap);

		if (role == Role.Customer) {
			response.sendRedirect("http://localhost:8080/Bank_Application/dashboard.html");
		} else {
			response.sendRedirect("http://localhost:8080/Bank_Application/emp-dashboard.html");
		}
	}

	private void createOrUpdate(OauthProvider oauthProvider, String sessionId) throws Exception {
		Map<String, Object> oauthProviderMap = new HashMap<>();
		oauthProviderMap.put("userId", oauthProvider.getUserId());
		oauthProviderMap.put("provider", oauthProvider.getProvider());
		List<OauthProvider> oauthProviders = oauthProviderDAO.get(oauthProviderMap);
		CacheUtil.saveWithTTL(oauthProvider.getUserId().toString(), oauthProvider.getAccessToken(), 3600);

		long providerId = 0;
		if (!oauthProviders.isEmpty()) {
			updateOauthTokens(oauthProvider, oauthProviderMap);
			providerId = oauthProviders.get(0).getId();
		} else {
			providerId = oauthProviderDAO.create(oauthProvider);
		}
		LoginController.getInstance().saveSessionId(sessionId, oauthProvider.getUserId(), providerId);
	}

	private void updateOauthTokens(OauthProvider oauthProvider, Map<String, Object> oauthProviderMap) throws Exception {
		List<String> fields = new ArrayList<>(Arrays.asList("createdAt", "accessToken", "expiresIn"));
		List<Object> values = new ArrayList<>(Arrays.asList(System.currentTimeMillis(), oauthProvider.getAccessToken(),
				oauthProvider.getExpiresIn()));

		if (oauthProvider.getRefreshToken() != null) {
			fields.add("refreshToken");
			values.add(oauthProvider.getRefreshToken());
		}

		ColumnCriteria columnCriteria = new ColumnCriteria().setFields(fields).setValues(values);
		oauthProviderDAO.update(columnCriteria, oauthProviderMap);
	}

	private void updateAccessToken(String accessToken, String refreshToken) throws Exception {
		List<String> fields = new ArrayList<>(Arrays.asList("createdAt", "accessToken"));
		List<Object> values = new ArrayList<>(Arrays.asList(System.currentTimeMillis(), accessToken));

		ColumnCriteria columnCriteria = new ColumnCriteria().setFields(fields).setValues(values);
		Map<String, Object> oauthProviderMap = new HashMap<String, Object>();
		oauthProviderMap.put("refreshToken", refreshToken);
		oauthProviderDAO.update(columnCriteria, oauthProviderMap);
	}

	private void setCookie(HttpServletResponse response, User user, String sessionId, Map<String, Object> userMap)
			throws CustomException {
		Role role = Role.fromString(user.getRole());

		Helper.setCookie(response, "phone", user.getPhone(), 604800, false);
		Helper.setCookie(response, "email", user.getEmail(), 604800, false);
		Helper.setCookie(response, "sessionId", sessionId, 604800, true);
		Helper.setCookie(response, "fullname", user.getFullname(), 604800, false);
		Helper.setCookie(response, "status", user.getStatus(), 604800, false);
		Helper.setCookie(response, "role", role.toString(), 604800, false);
		Helper.setCookie(response, "id", user.getId(), 604800, false);

		if (role != Role.Customer) {
			userService.addStaffDetails(userMap, user);
			Helper.setCookie(response, "branchId", userMap.get("branchId"), 604800, false);
		}
	}

}
