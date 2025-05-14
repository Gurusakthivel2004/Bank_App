package crm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dao.DAO;
import dao.DaoFactory;
import enums.Constants.HttpStatusCodes;
import enums.Constants.ModuleCode;
import enums.Constants.SymbolProvider;
import io.github.cdimascio.dotenv.Dotenv;
import model.ColumnCriteria;
import model.OauthClientConfig;
import model.OauthProvider;
import model.Org;
import model.SubOrg;
import model.User;
import util.CRMQueueManager;
import util.CustomException;
import util.Helper;
import util.HttpUtil;
import util.OAuthConfig;

public class CRMService {

	private AccountsService accountService = AccountsService.getInstance();
	private ContactsService contactService = ContactsService.getInstance();
	private DealsService dealsService = DealsService.getInstance();
	private LeadsService leadService = LeadsService.getInstance();

	private static Dotenv dotenv = Helper.loadDotEnv();
	private static final String SCOPE = OAuthConfig.get("crm.scope");
	private static final String SOID = "ZohoCrm." + OAuthConfig.get("crm.orgId");
	public static final String PROVIDER = "Zoho";
	private static final String ACCOUNT_URL = dotenv.get("ZOHO_ACCOUNTS_URL");

	private CRMService() {}

	private static class SingletonHelper {
		private static final CRMService INSTANCE = new CRMService();
	}

	public static CRMService getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public String pushAccountRecords(Org org, User user) throws Exception {
		return accountService.pushOrgToCRM(org, user);
	}

	public void pushContactRecords(User user, String accountId) throws Exception {
		contactService.pushContact(user, accountId);
	}

	public void pushDealsRecords(String moduleName, String amount, String moduleId, Long userId, Org org)
			throws Exception {
		dealsService.pushModuleToCRM(moduleName, amount, moduleId, userId, org);
	}

	public String pushLeadsRecords(SubOrg subOrg, String company, String email) throws Exception {
		return leadService.pushLead(subOrg, company, email);
	}

	public void refreshAccessToken() throws Exception {
		OauthClientConfig clientConfig = Helper.getClientConfig(PROVIDER);
		DAO<OauthProvider> oauthProviderDao = DaoFactory.getDAO(OauthProvider.class);

		String url = ACCOUNT_URL + OAuthConfig.get("oauth.token.url") + "?" + "grant_type=client_credentials&scope="
				+ SCOPE + "&soid=" + SOID;

		String tokenResponse = HttpUtil.sendPostRequestProxy(url, Helper.toJson(new HashMap<String, Object>()), null,
				clientConfig.getClientId(), clientConfig.getClientSecret());

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

	public <K extends Enum<K> & SymbolProvider> void updateRecords(String criteriaKey, Object criteriaValue,
			Map<K, Object> updateFields, String moduleName) throws Exception {

		ModuleCode moduleCode = ModuleCode.valueOf(moduleName.toUpperCase());
		if (moduleCode == null) {
			throw new IllegalArgumentException("Invalid module name: " + moduleName);
		}

		Map<String, Object> flatFields = new HashMap<>();
		for (Map.Entry<K, Object> entry : updateFields.entrySet()) {
			flatFields.put(entry.getKey().getSymbol(), entry.getValue());
		}

		Map<String, Object> payload = new HashMap<>();
		payload.put("Module_Code", moduleCode.getId());
		payload.put("Criteria_Key", criteriaKey);
		payload.put("Criteria_Value", criteriaValue);
		payload.put("Update_Fields", flatFields);

		CRMQueueManager.addToUpdateSet(payload);
	}

}