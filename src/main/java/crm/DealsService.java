package crm;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cache.CacheUtil;
import enums.Constants.DealsFields;
import enums.Constants.Module;
import enums.Constants.TaskExecutor;
import enums.Constants.UseCase;
import model.Org;
import model.User;
import util.CustomException;
import util.Helper;
import util.JsonUtils;
import util.OAuthConfig;

public class DealsService {

	private CRMHttpService crmHttpService = CRMHttpService.getInstance();
	private static final String DEAL_ENDPOINT = OAuthConfig.get("crm.deals.endpoint");
	private static final Logger LOGGER = LogManager.getLogger(DealsService.class);

	public static final String CRM_MODULE = "Deals";
	public static final String CRM_MODULE_PK = "Module Record Id";

	private DealsService() {}

	private static class SingletonHelper {
		private static final DealsService INSTANCE = new DealsService();
	}

	public static DealsService getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public Long pushModuleToCRM(String moduleName, String amount, String moduleRecordId, User user, Org org,
			boolean logFailedRequest) throws Exception {
		TaskExecutor.CRM.submitTask(() -> {
			try {
				setOrg(moduleName);
				String accountId = AccountsService.getInstance().insertIfAbsent(org);

				String contactId = ContactsService.getInstance().insertIfAbsent(org, user, accountId);
				String dealId = insertIfAbsent(moduleName, amount, moduleRecordId, org, accountId, contactId);

				return dealId;

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
			} finally {
				Helper.getThreadLocal().remove("org");
			}
			return null;
		});
		return null;
	}

	private String insertIfAbsent(String moduleName, String amount, String moduleRecordId, Org org, String accountId,
			String contactId) throws Exception {
		// Fetch Deals
		String dealsJsonResponse = crmHttpService.fetchRecord(DEAL_ENDPOINT, "Module_Record_Id", moduleRecordId);

		String dealId = JsonUtils.getValueByPath(dealsJsonResponse, "data[0]", "id");
		// Push deals record
		if (dealId == null) {
			dealId = pushDealsRecords(moduleName, amount, org.getName(), moduleRecordId, accountId, contactId);
		}

		return dealId;
	}

	public String updateRecord(String updateJson) throws Exception {
		String dealsJsonResponse = crmHttpService.putToCrm(DEAL_ENDPOINT, updateJson);
		return dealsJsonResponse;
	}

	private String pushDealsRecords(String dealName, String amount, String accountName, String moduleRecordId,
			String accountId, String contactId) throws Exception {

		Map<DealsFields, Object> data = new HashMap<>();
		data.put(DealsFields.FK_ACCOUNT_NAME, accountId);
		data.put(DealsFields.FK_CONTACT_NAME, contactId);
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

	private void setOrg(String moduleName) throws CustomException {
		Module module = Module.fromString(moduleName.replaceAll(" ", ""));
		String org = null;

		switch (module) {
		case FixedDeposit:
			org = OAuthConfig.get("fixedDeposit.org");
			break;
		case Loan:
			org = OAuthConfig.get("loan.org");
			break;
		default:
			LOGGER.error("Invalid module name.");
			break;
		}
		Helper.getThreadLocal().put("org", org);
	}

}