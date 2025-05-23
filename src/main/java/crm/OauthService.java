package crm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dao.DAO;
import dao.DaoFactory;
import enums.Constants.HttpStatusCodes;
import io.github.cdimascio.dotenv.Dotenv;
import model.ColumnCriteria;
import model.OauthClientConfig;
import model.OauthProvider;
import util.CustomException;
import util.Helper;
import util.HttpUtil;
import util.OAuthConfig;

public class OauthService {

	private static Dotenv dotenv = Helper.loadDotEnv();
	private static final String SCOPE = OAuthConfig.get("crm.scope");
	private static final String DEFAULT_ORG = "CDP-PT";
	public static final String PROVIDER = "Zoho";
	private static final String ACCOUNT_URL = dotenv.get("ZOHO_ACCOUNTS_URL");

	private OauthService() {}

	private static class SingletonHelper {
		private static final OauthService INSTANCE = new OauthService();
	}

	public static OauthService getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public void refreshAccessToken() throws Exception {

		OauthClientConfig clientConfig = Helper.getClientConfig(PROVIDER);
		DAO<OauthProvider> oauthProviderDao = DaoFactory.getDAO(OauthProvider.class);

		String org = (String) Helper.getThreadLocalValue("org");
		String soid = "ZohoCrm." + OAuthConfig.get("crm." + DEFAULT_ORG + ".orgId");
		if (org != null) {
			soid = "ZohoCrm." + OAuthConfig.get("crm." + org + ".orgId");
		}

		String url = ACCOUNT_URL + OAuthConfig.get("oauth.token.url") + "?" + "grant_type=client_credentials&scope="
				+ SCOPE + "&soid=" + soid;

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

		if (org != null) {
			providerMap.put("org", org);
		}

		oauthProviderDao.update(columnCriteria, providerMap);
	}

}
