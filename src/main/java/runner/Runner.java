package runner;

import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dao.DAO;
import dao.DaoFactory;
import initializer.Initializer;
import model.OauthProvider;
import util.Helper;
import util.HttpUtil;
import util.UsernameValidator;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class Runner {

	private static DAO<OauthProvider> oauthProviderDao = DaoFactory.getDAO(OauthProvider.class);

	public static void main(String[] args) {
//		NotificationService.getInstance().sendEmail("subi", "asd", "asd");
		try {
//			TransactionService.getInstance().updateTransactionStatus(5519l, TransactionStatus.Completed);
//			generateTokens();

			Initializer.setDataSource();
//			CRMService.getInstance().refreshAccessToken();
//			Map<String, Object> userMap = new HashMap<String, Object>();
//			
//			userMap.put("userId", 152l);
//			userMap.put("role", "Customer");
//			Map<String, Object> users = UserService.getInstance().getUserDetails(userMap);
//			Staff staff = users.get("users");
//			System.out.println(((List<Object>) users.get("users")).get(0).getClass());
//			
//			CustomerDetail customerDetail = new CustomerDetail();
//
//			customerDetail.setCustomerId(12345L).setDob("1990-01-01").setFatherName("John Doe")
//					.setMotherName("Jane Doe").setAddress("123 Main St, Springfield").setMaritalStatus("Single")
//					.setUserId(1L).setPanNumber("ABCDE1234F").setAadharNumber(123456789012L).setFullname("Alex Smith")
//					.setEmail("alex.smith@example.com").setPhone(9876543210L).setUsername("alexsmith")
//					.setPassword("securepassword").setRoleEnum(Role.Customer).setStatusEnum(Status.Active)
//					.setCreatedAt(System.currentTimeMillis()).setModifiedAt(System.currentTimeMillis())
//					.setPerformedBy(1L).setPasswordVersion(1);

//			OauthClientConfig config = Helper.getClientConfig("Zoho");
//			String jsonResponse = CRMService.getInstance().fetchRecords(OAuthConfig.get("crm.account.endpoint"), "Account_Name", "Zoho", config);
//			Map<ContactsFields, Object> userMap = new HashMap<ContactsFields, Object>();
//			userMap.put(ContactsFields.EMAIL, "vijayguru2004@zoho.com");
//			userMap.put(ContactsFields.PHONE, "9361409787");
//
//			CRMService.getInstance().updateRecords("Email", "guruvijay@zoho.com", userMap, "Contacts");
//			Map<AccountsFields, Object> accMap = new HashMap<AccountsFields, Object>();
//			accMap.put(AccountsFields.PHONE, "9361409787");
//			
//			CRMService.getInstance().updateRecords("Account_Name", "Zoho", accMap, "Accounts");
//			OauthClientConfig config = Helper.getClientConfig("Zoho");
//			String jsonResponse = CRMService.getInstance().fetchRecords(OAuthConfig.get("crm.contact.endpoint"), "Account_Name", "Zoho",
//					config);
//			System.out.println(jsonResponse);

			Integer score = UsernameValidator.validateUsername("username123");
			System.out.println(score);
//			System.out.println("json response: " + jsonResponse);
//
//			pushAccountRecords(customerDetail);
//			CRMService.getInstance().pushAccountRecords(customerDetail);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static void generateTokens() throws Exception {
		String url = "https://accounts.localzoho.com/oauth/v2/token?" + "grant_type=client_credentials&"
				+ "scope=zohocrm.settings.all,zohocrm.modules.all,zohocrm.users.all,zohocrm.org.all&"
				+ "soid=ZohoCrm.103791165";
		Map<String, Object> postMap = new HashMap<>();
//		System.out.println(Helper.toJson(postMap));
		String tokenResponse = HttpUtil.sendPostRequestProxy(url, Helper.toJson(postMap), null,
				"1000.AB2FWCIBJZOJMWJNDTW1LASSDY300X", "965c0de34496edf613c3cd43017d20213b59bddfe0");
		JsonObject tokenJson = JsonParser.parseString(tokenResponse).getAsJsonObject();
		System.out.println(tokenJson);
	}
}
