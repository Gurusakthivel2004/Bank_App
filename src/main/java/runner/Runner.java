package runner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dao.DAO;
import dao.DaoFactory;
import enums.Constants.Role;
import enums.Constants.Status;
import initializer.Initializer;
import model.CustomerDetail;
import model.OauthClientConfig;
import model.OauthProvider;
import service.CRMService;
import service.UserService;
import util.Helper;
import util.OAuthConfig;

public class Runner {

	private static DAO<OauthProvider> oauthProviderDao = DaoFactory.getDAO(OauthProvider.class);

	public static void main(String[] args) {
//		NotificationService.getInstance().sendEmail("subi", "asd", "asd");
		try {
//			TransactionService.getInstance().updateTransactionStatus(5519l, TransactionStatus.Completed);
//			generateTokens();
		
			Initializer.setDataSource();
//			Map<String, Object> userMap = new HashMap<String, Object>();
//			
//			userMap.put("userId", 152l);
//			userMap.put("role", "Customer");
//			Map<String, Object> users = UserService.getInstance().getUserDetails(userMap);
////			Staff staff = users.get("users");
//			System.out.println(((List<Object>) users.get("users")).get(0).getClass());
//			
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
			
			OauthClientConfig config = Helper.getClientConfig("Zoho");
			String jsonResponse = CRMService.getInstance().fetchRecords(OAuthConfig.get("crm.account.endpoint"), "Account_Name", "Zoho", config);
			System.out.println("json response: " + jsonResponse);
//
//			pushAccountRecords(customerDetail);
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
}
