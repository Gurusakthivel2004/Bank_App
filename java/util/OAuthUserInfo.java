package util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class OAuthUserInfo {
	public static String getUserInfo(String accessToken) throws Exception {
		String userInfoUrl = "https://www.googleapis.com/oauth2/v3/userinfo";
		URL url = new URL(userInfoUrl);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Authorization", "Bearer " + accessToken);

		if (conn.getResponseCode() != 200) {
			throw new RuntimeException("Failed to fetch user info: HTTP error code " + conn.getResponseCode());
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		StringBuilder response = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null) {
			response.append(line);
		}
		br.close();

		return response.toString();
	}

	public static String extractEmail(String accessToken) throws Exception {
		String jsonResponse = getUserInfo(accessToken);

		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode jsonNode = objectMapper.readTree(jsonResponse);

		return jsonNode.get("email").asText();
	}
}
