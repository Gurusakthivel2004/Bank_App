package util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import model.OauthClientConfig;

public class UsernameValidator {

	private static final String OPENAI_API_URL = OAuthConfig.get("openai.api_url");
	private static final String PROVIDER = "openai";
	private static final ObjectMapper mapper = new ObjectMapper();

	public static double validateUsername(String username) throws Exception {
		String prompt = buildPrompt(username);

		String requestBody = "{" + "\"model\": \"gpt-3.5-turbo\","
				+ "\"messages\": [{\"role\": \"user\", \"content\": \"" + prompt + "\"}]," + "\"temperature\": 0" + "}";

		OauthClientConfig clientConfig = Helper.getClientConfig(PROVIDER);
		String OpenaiApiKey = clientConfig.getClientSecret();
		String response = HttpUtil.sendPostRequest(OPENAI_API_URL, requestBody, OpenaiApiKey);

		JsonNode rootNode = mapper.readTree(response);
		String replyContent = rootNode.path("choices").get(0).path("message").path("content").asText();

		return Double.parseDouble(replyContent.trim());
	}

	private static String buildPrompt(String username) {
		return "Classify this username for spam likelihood. "
				+ "Give a score between 0 (definitely spammy) and 1 (perfectly genuine). "
				+ "Only respond with a floating point number. Username: " + username;
	}
}
