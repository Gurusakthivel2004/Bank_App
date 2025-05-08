package crm;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dao.DAO;
import dao.DaoFactory;
import enums.Constants.FieldIdentifier;
import enums.Constants.HttpMethod;
import enums.Constants.RetryStatus;
import enums.Constants.SymbolProvider;
import io.github.cdimascio.dotenv.Dotenv;
import model.FailedRequest;
import model.OauthProvider;
import util.CRMQueueManager;
import util.Helper;
import util.HttpUtil;
import util.JsonUtils;

public class CRMHttpService {

	private static final Logger logger = LogManager.getLogger(CRMHttpService.class);
	private static final Dotenv dotenv = Helper.loadDotEnv();

	private static final String PROVIDER = "Zoho";
	private static final String API_DOMAIN = dotenv.get("ZOHO_API_DOMAIN");

	private boolean isUnauthorized(Exception e) {
		return e.getMessage() != null && e.getMessage().contains("401");
	}

	private boolean isForbidden(Exception e) {
		return e.getMessage() != null && e.getMessage().contains("403");
	}

	private void logFailedRequest(String url, HttpMethod method, String jsonBody, Exception e) throws Exception {
		DAO<FailedRequest> failedRequestDao = DaoFactory.getDAO(FailedRequest.class);

		FailedRequest failedRequest = new FailedRequest();
		failedRequest.setUrl(url);
		failedRequest.setMethod(method.name());
		failedRequest.setRequestBody(jsonBody);
		failedRequest.setStatusCode(403);
		failedRequest.setErrorMessage(e.getMessage());
		failedRequest.setRetryStatus(RetryStatus.PENDING);
		failedRequest.setCreatedAt(System.currentTimeMillis());

		failedRequestDao.create(failedRequest);
	}

	private String sendWithRetry(HttpMethod method, String url, String jsonBody, OauthProvider provider)
			throws Exception {
		try {
			switch (method) {
			case POST:
				return HttpUtil.sendPostRequestProxy(url, jsonBody, provider.getAccessToken(), null, null);
			case GET:
				return HttpUtil.sendGetRequestProxy(url, provider.getAccessToken(), null, null);
			case PUT:
				return HttpUtil.sendPutRequestProxy(url, jsonBody, provider.getAccessToken(), null, null);
			default:
				logger.error("Unsupported HTTP method: {}", method);
				return null;
			}
		} catch (IOException e) {
			logger.error("Request failed: {}", e.getMessage());
			if (isUnauthorized(e)) {
				logger.info("Access token might be expired. Attempting to refresh...");
				CRMService.getInstance().refreshAccessToken();
			} else if (isForbidden(e)) {
				logger.warn("Received 403 Forbidden. Logging the failed request. URL: {}, Method: {}, Body: {}", url,
						method, jsonBody);
				logFailedRequest(url, method, jsonBody, e);
			} else {
				logger.error("Request to {} failed with error: {}", url, e.getMessage());
			}

			throw e;
		}
	}
	
	public String fetchRecord(String criteriaKey, String criteriaValue, String endpoint) throws Exception {
		OauthProvider provider = Helper.fetchOauthProvider(PROVIDER);

		String format = "%s%s/search?criteria=((%s:equals:%s))";
		String url = String.format(format, API_DOMAIN, endpoint, criteriaKey, criteriaValue);

		String jsonResponse = sendWithRetry(HttpMethod.GET, url, null, provider);
		logger.info("JSON response: {}", jsonResponse);

		return JsonUtils.getValueByPath(jsonResponse, "data[0]", "id");
	}

	public <K extends SymbolProvider> String postToCrm(String endpointKey, Map<K, Object> data) throws Exception {
		OauthProvider provider = Helper.fetchOauthProvider(PROVIDER);
		String url = API_DOMAIN + endpointKey;
		String jsonBody = JsonUtils.buildModuleJsonFromMap(data);

		return sendWithRetry(HttpMethod.POST, url, jsonBody, provider);
	}

	public String putToCrm(String endpointKey, String updateJson) throws Exception {
		OauthProvider provider = Helper.fetchOauthProvider(PROVIDER);
		String url = API_DOMAIN + endpointKey;

		return sendWithRetry(HttpMethod.PUT, url, updateJson, provider);
	}
	
	public <K extends Enum<K> & SymbolProvider> void updateRecords(String recordId, Map<K, Object> updateFields,
			String moduleName, String endpointKey) throws Exception {

		updateFields.put(Enum.valueOf(updateFields.keySet().iterator().next().getDeclaringClass(), "ID"), recordId);

		Map<K, Object> structuredFields = new HashMap<>();
		for (Map.Entry<K, Object> entry : updateFields.entrySet()) {
			K field = entry.getKey();
			Object value = entry.getValue();

			if ("ID".equals(field.name())) {
				structuredFields.put(field, value);
			} else {
				Map<String, Object> fieldValueMap = new HashMap<>();
				fieldValueMap.put("value", value);

				Optional<FieldIdentifier> identifierOpt = FieldIdentifier.fromModuleAndField(moduleName,
						field.getSymbol());
				if (identifierOpt.isPresent()) {
					fieldValueMap.put("identifier", identifierOpt.get().getId());
				}

				structuredFields.put(field, fieldValueMap);
			}
		}

		CRMQueueManager.addToUpdateSet(structuredFields);
	}
	
}