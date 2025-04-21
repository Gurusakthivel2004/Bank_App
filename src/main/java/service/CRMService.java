package service;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dao.DAO;
import dao.DaoFactory;
import enums.Constants.AccountsFields;
import enums.Constants.ContactsFields;
import enums.Constants.HttpStatusCodes;
import enums.Constants.SymbolProvider;
import io.github.cdimascio.dotenv.Dotenv;
import model.ColumnCriteria;
import model.CustomerDetail;
import model.OauthClientConfig;
import model.OauthProvider;
import util.CustomException;
import util.Helper;
import util.OAuthConfig;

public class CRMService {

	private static Logger logger = LogManager.getLogger(CRMService.class);
	private DAO<OauthProvider> oauthProviderDao = DaoFactory.getDAO(OauthProvider.class);

	private static Dotenv dotenv = Helper.loadDotEnv();
	private static final String PROVIDER = "Zoho";
	private static final String API_DOMAIN = dotenv.get("ZOHO_API_DOMAIN");
	private static final String ACCOUNT_URL = dotenv.get("ZOHO_ACCOUNTS_URL");
	private static final String SCOPE = OAuthConfig.get("crm.scope");
	private static final String SOID = "ZohoCrm." + OAuthConfig.get("crm.orgId");

	private CRMService() {
	}

	private static class SingletonHelper {
		private static final CRMService INSTANCE = new CRMService();
	}

	public static CRMService getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public String getFieldsMetaData(String module, String accessToken) throws Exception {
		String url = API_DOMAIN + OAuthConfig.get("crm.fields.metadata.endpoint") + module;

		String tokenResponse = Helper.sendGetRequest(url, accessToken);
		JsonObject jsonResponse = JsonParser.parseString(tokenResponse).getAsJsonObject();
		JsonArray fieldsArray = jsonResponse.getAsJsonArray("fields");
		StringBuilder sb = new StringBuilder();

		for (JsonElement element : fieldsArray) {
			JsonObject field = element.getAsJsonObject();
			if (field.has("api_name")) {
				sb.append(field.get("api_name")).append(",");
			}
		}

		return sb.toString();
	}

	public void pushAccountRecords(CustomerDetail user) throws Exception {
		OauthClientConfig config = Helper.getClientConfig(PROVIDER);

		Map<Object, Object> data = new HashMap<>();
		data.put(AccountsFields.USER_ID, OAuthConfig.get("crm.userId"));
		data.put(AccountsFields.ACCOUNT_NAME, user.getFullname());
		data.put(AccountsFields.ACCOUNT_TYPE, user.getRole());
		data.put(AccountsFields.RATING, user.getStatus());
		data.put(AccountsFields.PHONE, user.getPhone().toString());
		pushToCrm(OAuthConfig.get("crm.account.endpoint"), data, config);
		pushContactRecords(user, config);
	}

	public void pushContactRecords(CustomerDetail user, OauthClientConfig config) throws Exception {
		Map<Object, Object> data = new HashMap<>();
		data.put(ContactsFields.USER_ID, OAuthConfig.get("crm.userId"));
		data.put(ContactsFields.EMAIL, user.getEmail());
		data.put(ContactsFields.FIRST_NAME, user.getFullname());
		data.put(ContactsFields.LAST_NAME, user.getUsername());
		data.put(ContactsFields.DOB, user.getDob());
		data.put(ContactsFields.PHONE, user.getPhone().toString());

		pushToCrm(OAuthConfig.get("crm.contact.endpoint"), data, config);
	}

	public void pushToCrm(String endpointKey, Map<Object, Object> data, OauthClientConfig config) throws Exception {

		Map<String, Object> oauthMap = new HashMap<>();
		oauthMap.put("clientConfigId", config.getId());
		OauthProvider provider = oauthProviderDao.get(oauthMap).get(0);

		String json = buildModuleJsonFromMap(data);
		logger.info(json);
		String url = API_DOMAIN + endpointKey;

		sendWithRetry(url, json, config, provider);
	}

	public String sendWithRetry(String url, String jsonBody, OauthClientConfig clientConfig, OauthProvider provider)
			throws Exception {
		int retries = 0;
		String response = null;
		while (retries < 2) {
			try {
				response = Helper.sendPostRequestWithJsonProxy(url, jsonBody, provider.getAccessToken(), null, null);
				return response;
			} catch (IOException e) {
				logger.error("Request failed: {}", e.getMessage());
				if (e.getMessage() != null && e.getMessage().contains("401")) {
					logger.info("Access token might be expired. Attempting to refresh...");
					refreshAccessToken(clientConfig);
					retries++;
				} else {
					throw e;
				}
			}
		}
		logger.error("Request to {} failed after retries. Response: {}", url, response);
		return response;
	}

	public void refreshAccessToken(OauthClientConfig clientConfig) throws Exception {
		DAO<OauthProvider> oauthProviderDao = DaoFactory.getDAO(OauthProvider.class);

		String url = ACCOUNT_URL + OAuthConfig.get("oauth.token.url") + "?" + "grant_type=client_credentials&scope="
				+ SCOPE + "&soid=" + SOID;

		String tokenResponse = Helper.sendPostRequestWithJsonProxy(url, Helper.toJson(new HashMap<String, Object>()),
				null, clientConfig.getClientId(), clientConfig.getClientSecret());

		JsonObject tokenJson = JsonParser.parseString(tokenResponse).getAsJsonObject();

		if (!tokenJson.has("access_token")) {
			throw new CustomException("Access token missing from provider.", HttpStatusCodes.BAD_REQUEST);
		}

		String accessToken = tokenJson.get("access_token").getAsString();
		ColumnCriteria columnCriteria = new ColumnCriteria().setFields(Arrays.asList("accessToken"))
				.setValues(Arrays.asList(accessToken));

		Map<String, Object> providerMap = new HashMap<>();
		providerMap.put("clientConfigId", clientConfig.getId());

		oauthProviderDao.update(columnCriteria, providerMap);
	}

	public String buildModuleJsonFromMap(Map<Object, Object> dataMap) {
		JsonObject accountObj = new JsonObject();

		for (Map.Entry<Object, Object> entry : dataMap.entrySet()) {
			Object key = entry.getKey();
			Object value = entry.getValue();

			String extractedKey;

			if (key instanceof SymbolProvider) {
				extractedKey = ((SymbolProvider) key).getSymbol();
			} else if (key instanceof Enum) {
				extractedKey = ((Enum<?>) key).name();
			} else {
				extractedKey = key.toString();
			}

			if (!entry.getKey().equals("User_Id")) {
				if (value instanceof Number) {
					accountObj.addProperty(extractedKey, (Number) value);
				} else {
					accountObj.addProperty(extractedKey, value.toString());
				}
			}
		}

		JsonArray dataArray = new JsonArray();
		dataArray.add(accountObj);
		JsonObject root = new JsonObject();
		root.add("data", dataArray);

		return root.toString();
	}

}