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
import service.UserService;
import util.Helper;
import util.JsonUtils;
import util.OAuthConfig;

public class DealsService {

	private CRMHttpService crmHttpService = CRMHttpService.getInstance();
	public static final String CRM_MODULE = "Deals";
	public static final String CRM_MODULE_PK = "Module Record Id";
<<<<<<< HEAD
	private static final Logger LOGGER= LogManager.getLogger(DealsService.class);
	
=======
	private static final String DEAL_ENDPOINT = OAuthConfig.get("crm.deal.endpoint");
	private static final String CONTACT_ENDPOINT = OAuthConfig.get("crm.contact.endpoint");
	private static final Logger LOGGER = LogManager.getLogger(DealsService.class);

>>>>>>> 7e942af (CRMSchedular update)
	private DealsService() {}

	private static class SingletonHelper {
		private static final DealsService INSTANCE = new DealsService();
	}

	public static DealsService getInstance() {
		return SingletonHelper.INSTANCE;
	}
<<<<<<< HEAD
	
=======

>>>>>>> 7e942af (CRMSchedular update)
	public Long pushModuleToCRM(String moduleName, String amount, String moduleId, Long userId, Org org)
			throws Exception {
		TaskExecutor.CRM.submitTask(() -> {
			try {
				User user = UserService.getInstance().getUserById(userId);
				// Fetch Deals
<<<<<<< HEAD
				String dealsJsonResponse = crmHttpService.fetchRecord(OAuthConfig.get("crm.deal.endpoint"),
						"Module_Record_Id", moduleId);
=======
				String dealsJsonResponse = crmHttpService.fetchRecord(DEAL_ENDPOINT, "Module_Record_Id", moduleId);
>>>>>>> 7e942af (CRMSchedular update)

				String dealId = JsonUtils.getValueByPath(dealsJsonResponse, "data[0]", "id");
				// Push deals record
				if (dealId == null) {
					dealId = pushDealsRecords(moduleName, amount, org.getName(), moduleId);
				}
				// Push Accounts and Contacts if needed.
				AccountsService.getInstance().pushOrgToCRM(org, user);
			} catch (Exception e) {
				if (CRMHttpService.isForbidden(e)) {
					try {
						Map<String, String> jsonMap = new HashMap<>();
						jsonMap.put("orgId", org.getId().toString());
						jsonMap.put("userId", userId.toString());
						jsonMap.put("moduleRecordId", moduleId);
						jsonMap.put("useCase", UseCase.DEAL_PUSH.getId().toString());
<<<<<<< HEAD
						
=======

>>>>>>> 7e942af (CRMSchedular update)
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
<<<<<<< HEAD

	private String pushDealsRecords(String dealName, String amount, String accountName, String moduleRecordId) throws Exception {

		String jsonResponse = crmHttpService.fetchRecord(OAuthConfig.get("crm.contact.endpoint"), "Account_Name",
				accountName);
=======
	
	public String updateRecord(String updateJson) throws Exception {
		String dealsJsonResponse = crmHttpService.putToCrm(DEAL_ENDPOINT, updateJson);
		return dealsJsonResponse;
	}

	private String pushDealsRecords(String dealName, String amount, String accountName, String moduleRecordId)
			throws Exception {

		String jsonResponse = crmHttpService.fetchRecord(CONTACT_ENDPOINT, "Account_Name", accountName);
>>>>>>> 7e942af (CRMSchedular update)

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

<<<<<<< HEAD
		String response = crmHttpService.postToCrm(OAuthConfig.get("crm.deal.endpoint"), data);
		String recordId = JsonUtils.getValueByPath(response, "data[0].details", "id");
		CacheUtil.saveCRMRecordId(CRM_MODULE, moduleRecordId, recordId);
		
		return recordId;
	}
=======
		String response = crmHttpService.postToCrm(DEAL_ENDPOINT, data);
		String recordId = JsonUtils.getValueByPath(response, "data[0].details", "id");
		CacheUtil.saveCRMRecordId(CRM_MODULE, moduleRecordId, recordId);

		return recordId;
	}
	
>>>>>>> 7e942af (CRMSchedular update)
}