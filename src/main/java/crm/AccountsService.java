package crm;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cache.CacheUtil;
import enums.Constants.AccountsFields;
import enums.Constants.TaskExecutor;
import enums.Constants.UseCase;
import model.Org;
import model.User;
import util.Helper;
import util.JsonUtils;
import util.OAuthConfig;

public class AccountsService {

	public static final String CRM_MODULE = "Accounts";
	public static final String CRM_MODULE_PK = "Phone";
	private static final String ACCOUNT_ENDPOINT = OAuthConfig.get("crm.accounts.endpoint");

	private static CRMHttpService crmHttpService = CRMHttpService.getInstance();
	private static final Logger LOGGER = LogManager.getLogger(AccountsService.class);

	private AccountsService() {}

	private static class SingletonHelper {
		private static final AccountsService INSTANCE = new AccountsService();
	}

	public static AccountsService getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public String pushOrgToCRM(Org org, User user, boolean logFailedRequest) {
		TaskExecutor.CRM.submitTask(() -> {
			try {
				return lookupsAndPush(org, user);
			} catch (Exception e) {
				if (logFailedRequest) {
					LOGGER.warn("Received 403 Forbidden. Logging the failed request.");
					try {
						Map<String, Object> jsonMap = new HashMap<>();
						jsonMap.put("orgId", org.getId().toString());
						jsonMap.put("userId", user.getId().toString());
						jsonMap.put("useCase", UseCase.ORG_PUSH.getId().toString());

						Helper.logFailedRequest(jsonMap);
					} catch (Exception exception) {
						LOGGER.error(exception.getMessage());
					}
				}
				LOGGER.error("CRM push failed: {}", e.getMessage(), e);
			}
			return null;
		});
		return null;
	}
	
	public String insertIfAbsent(Org org) throws Exception {
		// Fetch accounts
		String accountsJsonResponse = crmHttpService.fetchRecord(ACCOUNT_ENDPOINT, "Account_Name", org.getName());

		String accountId = JsonUtils.getValueByPath(accountsJsonResponse, "data[0]", "id");
		// Push accounts record
		if (accountId == null) {
			accountId = pushAccountsRecord(org);
		}
		
		return accountId;
	}

	private String lookupsAndPush(Org org, User user) throws Exception {
		String accountId = insertIfAbsent(org);
		
		String contactId = ContactsService.getInstance().insertIfAbsent(org, user, accountId);
		
		return contactId;
	}

	public String updateRecord(String updateJson) throws Exception {
		String accountsJsonResponse = crmHttpService.putToCrm(ACCOUNT_ENDPOINT, updateJson);
		return accountsJsonResponse;
	}

	private String pushAccountsRecord(Org org) throws Exception {
		String phone = org.getPhone().toString();

		Map<AccountsFields, Object> data = new HashMap<>();
		data.put(AccountsFields.ACCOUNT_NAME, org.getName());
		data.put(AccountsFields.INDUSTRY, org.getOrgType());
		data.put(AccountsFields.EMPLOYEES, org.getEmployees());
		data.put(AccountsFields.PHONE, phone);

		String response = crmHttpService.postToCrm(ACCOUNT_ENDPOINT, data);
		String recordId = JsonUtils.getValueByPath(response, "data[0].details", "id");
		CacheUtil.saveCRMRecordId(CRM_MODULE, phone, recordId);

		return recordId;
	}

}