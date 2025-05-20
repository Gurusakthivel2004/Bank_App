package crm;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cache.CacheUtil;
import enums.Constants.DealsFields;
import enums.Constants.TaskExecutor;
import enums.Constants.UseCase;
import model.Org;
import model.User;
import util.Helper;
import util.JsonUtils;
import util.OAuthConfig;

public class DealsService {

	private CRMHttpService crmHttpService = CRMHttpService.getInstance();
	public static final String CRM_MODULE = "Deals";
	public static final String CRM_MODULE_PK = "Module Record Id";
	private static final String DEAL_ENDPOINT = OAuthConfig.get("crm.deals.endpoint");
	private static final String CONTACT_ENDPOINT = OAuthConfig.get("crm.contacts.endpoint");
	private static final Logger LOGGER = LogManager.getLogger(DealsService.class);

	private DealsService() {}

	private static class SingletonHelper {
		private static final DealsService INSTANCE = new DealsService();
	}

	public static DealsService getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public Long pushModuleToCRM(String moduleName, String amount, String moduleRecordId, User user, Org org, boolean logFailedRequest)
			throws Exception {
		TaskExecutor.CRM.submitTask(() -> {
			try {
				// Fetch Deals
				String dealsJsonResponse = crmHttpService.fetchRecord(DEAL_ENDPOINT, "Module_Record_Id", moduleRecordId);

				String dealId = JsonUtils.getValueByPath(dealsJsonResponse, "data[0]", "id");
				// Push deals record
				if (dealId == null) {
					dealId = pushDealsRecords(moduleName, amount, org.getName(), moduleRecordId);
				}
				// Push Accounts and Contacts if needed.
				AccountsService.getInstance().pushOrgToCRM(org, user, true);
			} catch (Exception e) {
				if (logFailedRequest) {
					try {
						Map<String, Object> jsonMap = new HashMap<>();
						jsonMap.put("orgId", org.getId().toString());
						jsonMap.put("userId", user.getId().toString());
						jsonMap.put("moduleName", moduleName);
						jsonMap.put("amount", amount);
						jsonMap.put("moduleRecordId", moduleRecordId);
						jsonMap.put("useCase", UseCase.DEAL_PUSH.getId().toString());

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

	public String updateRecord(String updateJson) throws Exception {
		String dealsJsonResponse = crmHttpService.putToCrm(DEAL_ENDPOINT, updateJson);
		return dealsJsonResponse;
	}

	private String pushDealsRecords(String dealName, String amount, String accountName, String moduleRecordId)
			throws Exception {

		String jsonResponse = crmHttpService.fetchRecord(CONTACT_ENDPOINT, "Account_Name", accountName);

		String accountId = JsonUtils.getValueByPath(jsonResponse, "data[0].Account_Name", "id");
		String contactId = JsonUtils.getValueByPath(jsonResponse, "data[0]", "id");

		Map<DealsFields, Object> data = new HashMap<>();
		data.put(DealsFields.FK_ACCOUNT_NAME, accountId);
		data.put(DealsFields.FK_Contact_NAME, contactId);
		data.put(DealsFields.MODULE_RECORD_ID, moduleRecordId);
		data.put(DealsFields.AMOUNT, amount);
		data.put(DealsFields.STAGE, "Needs Analysis");
		data.put(DealsFields.TYPE, "New Business");
		data.put(DealsFields.DEAL_NAME, dealName);

		String response = crmHttpService.postToCrm(DEAL_ENDPOINT, data);
		String recordId = JsonUtils.getValueByPath(response, "data[0].details", "id");
		CacheUtil.saveCRMRecordId(CRM_MODULE, moduleRecordId, recordId);

		return recordId;
	}

}