package runner;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dao.DAO;
import dao.DaoFactory;
import enums.Constants.AccountsFields;
import enums.Constants.Role;
import enums.Constants.Status;
import model.CustomerDetail;
import model.OauthProvider;
import service.CRMService;
import util.Helper;
import util.OAuthConfig;

public class Runner {

	private static DAO<OauthProvider> oauthProviderDao = DaoFactory.getDAO(OauthProvider.class);

	public static void main(String[] args) {
//		NotificationService.getInstance().sendEmail("subi", "asd", "asd");
		try {
//			TransactionService.getInstance().updateTransactionStatus(5519l, TransactionStatus.Completed);
//			generateTokens();
			CustomerDetail customerDetail = new CustomerDetail();

			customerDetail.setCustomerId(12345L).setDob("1990-01-01").setFatherName("John Doe")
					.setMotherName("Jane Doe").setAddress("123 Main St, Springfield").setMaritalStatus("Single")
					.setUserId(1L).setPanNumber("ABCDE1234F").setAadharNumber(123456789012L).setFullname("Alex Smith")
					.setEmail("alex.smith@example.com").setPhone(9876543210L).setUsername("alexsmith")
					.setPassword("securepassword").setRoleEnum(Role.Customer).setStatusEnum(Status.Active)
					.setCreatedAt(System.currentTimeMillis()).setModifiedAt(System.currentTimeMillis())
					.setPerformedBy(1L).setPasswordVersion(1);
//
			pushAccountRecords(customerDetail);
//			CRMService.getInstance().pushAccountRecords(customerDetail);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void generateTokens() throws Exception {
		String url = "https://accounts.localzoho.com/oauth/v2/token?" + "grant_type=client_credentials&"
				+ "scope=zohocrm.settings.all,zohocrm.modules.all,zohocrm.users.all,zohocrm.org.all&"
				+ "soid=ZohoCrm.103791165";
		Map<String, Object> postMap = new HashMap<>();
//		System.out.println(Helper.toJson(postMap));
		String tokenResponse = Helper.sendPostRequestWithJsonProxy(url, Helper.toJson(postMap), null,
				"1000.AB2FWCIBJZOJMWJNDTW1LASSDY300X", "965c0de34496edf613c3cd43017d20213b59bddfe0");
		JsonObject tokenJson = JsonParser.parseString(tokenResponse).getAsJsonObject();
		System.out.println(tokenJson);
	}

	private static void pushAccountRecords(CustomerDetail customer) throws Exception {
		Map<String, Object> oauthMap = new HashMap<String, Object>();
		oauthMap.put("providerUserId", 1111l);

		Map<Object, Object> data = new HashMap<>();
		data.put(AccountsFields.USER_ID, "103790419");
		data.put(AccountsFields.ACCOUNT_NAME, "Gurusakthivel");
		data.put(AccountsFields.ACCOUNT_TYPE, "Savings");
		data.put(AccountsFields.RATING, "rating");
		data.put(AccountsFields.PHONE, "9361409778");

		String jsonBody = CRMService.getInstance().buildModuleJsonFromMap(data);
		System.out.println(jsonBody);
		String url = "https://crm.localzoho.com/crm/v6/Accounts", response = null;

		int retries = 0;
		response = Helper.sendPostRequestWithJsonProxy(url, jsonBody,
				"1000.cd61213305ec73f226009d68e7c7b4fa.7de4a36ec8983982dbd2b968582a06b2", null, null);
		System.out.println(response);
		if (retries == 2) {
			System.out.println("Accounts push to CRM failed, response: {}" + response);
		}
	}
}
