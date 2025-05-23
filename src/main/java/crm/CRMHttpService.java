package crm;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import enums.Constants.HttpMethod;
import enums.Constants.SymbolProvider;
import io.github.cdimascio.dotenv.Dotenv;
import model.OauthProvider;
import util.Helper;
import util.HttpUtil;
import util.JsonUtils;

public class CRMHttpService {

	private static final Logger LOGGER = LogManager.getLogger(CRMHttpService.class);
	private static final Dotenv dotenv = Helper.loadDotEnv();

	private static final String PROVIDER = "Zoho";
	private static final String API_DOMAIN = dotenv.get("ZOHO_API_DOMAIN");

	private CRMHttpService() {}

	private static class SingletonHelper {
		private static final CRMHttpService INSTANCE = new CRMHttpService();
	}

	public static CRMHttpService getInstance() {
		return SingletonHelper.INSTANCE;
	}

	private boolean isUnauthorized(Exception e) {
		return e.getMessage() != null && e.getMessage().contains("401");
	}

	public static boolean isForbidden(Exception e) {
		return e.getMessage() != null && e.getMessage().contains("403");
	}

	public static boolean isServerError(Exception e) {
		return e.getMessage() != null && e.getMessage().contains("500");
	}
	
	private static OauthProvider getOauthProvider() throws Exception {
		String org = (String) Helper.getThreadLocalValue("org");
	
		OauthProvider provider = Helper.fetchOauthProvider(PROVIDER, org);
		return provider;
	}

	private String sendWithRetry(HttpMethod method, String url, String jsonBody)
			throws Exception {
		OauthProvider provider = getOauthProvider();
		System.out.println("org for this one is : " + provider.getOrg());
		try {
			String response = makeRequest(method, url, jsonBody, provider);
			LOGGER.info("resposne: " + response);
			return response;
		} catch (IOException e) {
			LOGGER.error("Request failed: {}", e.getMessage());
			if (isUnauthorized(e)) {
				LOGGER.info("Access token might be expired. Attempting to refresh...");
				OauthService.getInstance().refreshAccessToken();
				return makeRequest(method, url, jsonBody, provider);
			} else {
				LOGGER.error("Request to {} failed with error: {}", url, e.getMessage());
				throw e;
			}
		}
	}

	private String makeRequest(HttpMethod method, String url, String jsonBody, OauthProvider provider)
			throws Exception {
		switch (method) {
		case POST:
			return HttpUtil.sendPostRequestProxy(url, jsonBody, provider.getAccessToken(), null, null);
		case GET:
			return HttpUtil.sendGetRequestProxy(url, provider.getAccessToken(), null, null);
		case PUT:
			return HttpUtil.sendPutRequestProxy(url, jsonBody, provider.getAccessToken(), null, null);
		default:
			LOGGER.error("Unsupported HTTP method: {}", method);
		}
		return null;
	}

	public String uploadFileToCrm(File zipFile, String uploadUrl) throws Exception {
		OauthProvider provider = getOauthProvider();
		String response = HttpUtil.sendPostRequest(uploadUrl, zipFile, provider.getAccessToken(), "103791165");

		return response;
	}

	public String fetchRecord(String endpoint, String criteriaKey, Object criteriaValue) throws Exception {
		String format = "%s%s/search?criteria=((%s:equals:%s))";
		String url = String.format(format, API_DOMAIN, endpoint, criteriaKey, criteriaValue);

		String jsonResponse = sendWithRetry(HttpMethod.GET, url, null);
		LOGGER.info(jsonResponse);
		return jsonResponse;
	}

	public <K extends SymbolProvider> String postToCrm(String endpointKey, Map<K, Object> data) throws Exception {
		String jsonBody = JsonUtils.buildModuleJsonFromMap(data);
		return postToCrm(endpointKey, jsonBody);
	}

	public <K extends SymbolProvider> String postToCrm(String endpointKey, String jsonBody) throws Exception {
		String url = API_DOMAIN + endpointKey;

		return sendWithRetry(HttpMethod.POST, url, jsonBody);
	}

	public String putToCrm(String endpointKey, String updateJson) throws Exception {
		String url = API_DOMAIN + endpointKey;

		return sendWithRetry(HttpMethod.PUT, url, updateJson);
	}

}