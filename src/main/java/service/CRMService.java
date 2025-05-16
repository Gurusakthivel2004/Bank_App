//package service;
//
//import java.io.IOException;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Optional;
//
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
//import com.google.gson.JsonObject;
//import com.google.gson.JsonParser;
//
//import dao.DAO;
//import dao.DaoFactory;
//import enums.Constants.AccountsFields;
//import enums.Constants.ContactsFields;
//import enums.Constants.DealsFields;
//import enums.Constants.FieldIdentifier;
//import enums.Constants.HttpMethod;
//import enums.Constants.HttpStatusCodes;
//import enums.Constants.LeadsFields;
//import enums.Constants.SymbolProvider;
//import io.github.cdimascio.dotenv.Dotenv;
//import model.ColumnCriteria;
//import model.OauthClientConfig;
//import model.OauthProvider;
//import model.Org;
//import model.SubOrg;
//import model.User;
//import util.CRMQueueManager;
//import util.CustomException;
//import util.Helper;
//import util.HttpUtil;
//import util.JsonUtils;
//import util.OAuthConfig;
//
//public class CRMService {
//
//	private static Logger logger = LogManager.getLogger(CRMService.class);
//	private DAO<OauthProvider> oauthProviderDao = DaoFactory.getDAO(OauthProvider.class);
//
//	private static Dotenv dotenv = Helper.loadDotEnv();
//	private static final Integer RETRIES = 5;
//	public static final String PROVIDER = "Zoho";
//	private static final String API_DOMAIN = dotenv.get("ZOHO_API_DOMAIN");
//	private static final String ACCOUNT_URL = dotenv.get("ZOHO_ACCOUNTS_URL");
//	private static final String SCOPE = OAuthConfig.get("crm.scope");
//	private static final String SOID = "ZohoCrm." + OAuthConfig.get("crm.orgId");
//
//	private CRMService() {}
//
//	private static class SingletonHelper {
//		private static final CRMService INSTANCE = new CRMService();
//	}
//
//	public static CRMService getInstance() {
//		return SingletonHelper.INSTANCE;
//	}
//
//	public String pushAccountRecords(Org org) throws Exception {
//
//		Map<AccountsFields, Object> data = new HashMap<>();
//		data.put(AccountsFields.ACCOUNT_NAME, org.getName());
//		data.put(AccountsFields.INDUSTRY, org.getOrgType());
//		data.put(AccountsFields.EMPLOYEES, org.getEmployees());
//		data.put(AccountsFields.PHONE, org.getPhone().toString());
//
//		String response = postToCrm(OAuthConfig.get("crm.account.endpoint"), data);
//
//		return JsonUtils.getValueByPath(response, "data[0].details", "id");
//	}
//
//	public void pushContactRecords(User user, String accountId) throws Exception {
//
//		logger.info("Account ID :" + accountId);
//
//		Map<ContactsFields, Object> data = new HashMap<>();
//		data.put(ContactsFields.FK_ACCOUNT_NAME, accountId);
//		data.put(ContactsFields.EMAIL, user.getEmail());
//		data.put(ContactsFields.FIRST_NAME, user.getFullname());
//		data.put(ContactsFields.LAST_NAME, user.getUsername());
//		data.put(ContactsFields.PHONE, user.getPhone().toString());
//
//		postToCrm(OAuthConfig.get("crm.contact.endpoint"), data);
//	}
//
////	public String pushDealsRecords(String dealName, String amount, String accountName, String moduleRecordId) throws Exception {
////
////		OauthClientConfig config = Helper.getClientConfig(PROVIDER);
////		String jsonResponse = fetchRecords(OAuthConfig.get("crm.contact.endpoint"), "Account_Name", accountName,
////				config);
////
////		String accountId = JsonUtils.getValueByPath(jsonResponse, "data[0].Account_Name", "id");
////		String contactId = JsonUtils.getValueByPath(jsonResponse, "data[0]", "id");
////
////		Map<DealsFields, Object> data = new HashMap<>();
////		data.put(DealsFields.FK_ACCOUNT_NAME, accountId);
////		data.put(DealsFields.FK_Contact_NAME, contactId);
////		data.put(DealsFields.MODULE_RECORD_ID, moduleRecordId);
////		data.put(DealsFields.AMOUNT, amount);
////		data.put(DealsFields.STAGE, "Needs Analysis");
////		data.put(DealsFields.TYPE, "New Business");
////		data.put(DealsFields.DEAL_NAME, dealName);
////
////		String response = postToCrm(OAuthConfig.get("crm.deal.endpoint"), data);
////		
////		return JsonUtils.getValueByPath(response, "data[0].details", "id");
////	}
//
//	public String pushLeadsRecords(SubOrg subOrg, String company, String email) throws Exception {
//
//		Map<LeadsFields, Object> data = new HashMap<>();
//		data.put(LeadsFields.FIRST_NAME, subOrg.getName());
//		data.put(LeadsFields.LAST_NAME, "test");
//		data.put(LeadsFields.COMPANY, company);
//		data.put(LeadsFields.EMAIL, email);
//		String response = postToCrm(OAuthConfig.get("crm.lead.endpoint"), data);
//
//		return JsonUtils.getValueByPath(response, "data[0].details", "id");
//	}
//
//	public <K extends Enum<K> & SymbolProvider> void updateRecords(String recordId, Map<K, Object> updateFields,
//			String moduleName, String endpointKey) throws Exception {
//
//		updateFields.put((K) Enum.valueOf(updateFields.keySet().iterator().next().getDeclaringClass(), "ID"), recordId);
//		Map<K, Object> structuredFields = new HashMap<>();
//
//		for (Map.Entry<K, Object> entry : updateFields.entrySet()) {
//			K field = entry.getKey();
//			Object value = entry.getValue();
//
//			if (field.name().equals("ID")) {
//				structuredFields.put(field, value);
//			} else {
//				Map<String, Object> valueWithIdentifier = new HashMap<>();
//				valueWithIdentifier.put("value", value);
//
//				Optional<FieldIdentifier> idOpt = FieldIdentifier.fromModuleAndField(moduleName, field.getSymbol());
//				idOpt.ifPresent(identifier -> valueWithIdentifier.put("identifier", identifier.getId()));
//
//				structuredFields.put(field, valueWithIdentifier);
//			}
//		}
//
//		CRMQueueManager.addToUpdateSet(structuredFields);
//	}
//
//	public String fetchRecord(String criteriaKey, String criteriaValue, String endpoint) throws Exception {
//		OauthClientConfig config = Helper.getClientConfig(PROVIDER);
//		String jsonResponse = fetchRecords(endpoint, criteriaKey, criteriaValue, config);
//		String id = JsonUtils.getValueByPath(jsonResponse, "data[0]", "id");
//
//		return id;
//	}
//
//	private OauthProvider fetchOauthProvider() throws Exception {
//
//		OauthClientConfig config = Helper.getClientConfig(PROVIDER);
//		Map<String, Object> oauthMap = new HashMap<>();
//		oauthMap.put("clientConfigId", config.getId());
//
//		return oauthProviderDao.get(oauthMap).get(0);
//	}
//
//	public <K extends SymbolProvider> String postToCrm(String endpointKey, Map<K, Object> data) throws Exception {
//
//		OauthProvider provider = fetchOauthProvider();
//
//		String json = JsonUtils.buildModuleJsonFromMap(data);
//
//		String url = API_DOMAIN + endpointKey;
//		return sendWithRetry(HttpMethod.POST, url, json, provider);
//	}
//
//	public <K extends SymbolProvider> String putToCrm(String endpointKey, String updateJson) throws Exception {
//
//		OauthProvider provider = fetchOauthProvider();
//
//		String url = API_DOMAIN + endpointKey;
//		System.out.println(API_DOMAIN);
//		System.out.println(endpointKey);
//		return sendWithRetry(HttpMethod.PUT, url, updateJson, provider);
//	}
//
//	public String fetchRecords(String endpointKey, String criteriaKey, String criteriaValue, OauthClientConfig config)
//			throws Exception {
//
//		Map<String, Object> oauthMap = new HashMap<>();
//		oauthMap.put("clientConfigId", config.getId());
//
//		OauthProvider provider = oauthProviderDao.get(oauthMap).get(0);
//
//		String url = API_DOMAIN + endpointKey + "/search?criteria=((" + criteriaKey + ":equals:" + criteriaValue + "))";
//
//		String jsonResponse = sendWithRetry(HttpMethod.GET, url, null, provider);
//		logger.info("json response: " + jsonResponse);
//		return jsonResponse;
//	}
//
//	public void refreshAccessToken() throws Exception {
//		OauthClientConfig clientConfig = Helper.getClientConfig(PROVIDER);
//		DAO<OauthProvider> oauthProviderDao = DaoFactory.getDAO(OauthProvider.class);
//
//		String url = ACCOUNT_URL + OAuthConfig.get("oauth.token.url") + "?" + "grant_type=client_credentials&scope="
//				+ SCOPE + "&soid=" + SOID;
//
//		String tokenResponse = HttpUtil.sendPostRequestProxy(url, Helper.toJson(new HashMap<String, Object>()),
//				null, clientConfig.getClientId(), clientConfig.getClientSecret());
//
//		JsonObject tokenJson = JsonParser.parseString(tokenResponse).getAsJsonObject();
//
//		if (!tokenJson.has("access_token")) {
//			throw new CustomException("Access token missing from provider.", HttpStatusCodes.BAD_REQUEST);
//		}
//
//		String accessToken = tokenJson.get("access_token").getAsString();
//		ColumnCriteria columnCriteria = new ColumnCriteria().setFields(Arrays.asList("accessToken"))
//				.setValues(Arrays.asList(accessToken));
//
//		Map<String, Object> providerMap = new HashMap<>();
//		providerMap.put("clientConfigId", clientConfig.getId());
//
//		oauthProviderDao.update(columnCriteria, providerMap);
//	}
//
//	public String sendWithRetry(HttpMethod method, String url, String jsonBody, OauthProvider provider)
//			throws Exception {
//
//		int retries = 0;
//		String response = null;
//
//		while (retries < RETRIES) {
//			try {
//				switch (method) {
//				case POST:
//					response = HttpUtil.sendPostRequestProxy(url, jsonBody, provider.getAccessToken(), null,
//							null);
//					break;
//				case GET:
//					response = HttpUtil.sendGetRequestProxy(url, provider.getAccessToken(), null, null);
//					break;
//				case PUT:
//					response = HttpUtil.sendPutRequestProxy(url, jsonBody, provider.getAccessToken(), null, null);
//					break;
//				default: 
//					return null;
//				}
//
//				if (response != null && response.length() > 0) {
//					logger.info("returned response: " + response.length());
//					return response;
//				}
//			} catch (IOException e) {
//				logger.error("Request failed: {}", e.getMessage());
//
//				if (e.getMessage() != null && e.getMessage().contains("401")) {
//					logger.info("Access token might be expired. Attempting to refresh...");
//					refreshAccessToken();
//				}
//
//			}
//			retries++;
//		}
//
//		logger.error("Request to {} failed after retries. Response: {}", url, response);
//		return response;
//	}
//
//}