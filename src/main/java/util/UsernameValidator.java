package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.fasterxml.jackson.databind.ObjectMapper;

public class UsernameValidator {

	private static final String API_URL = "https://api.together.xyz/v1/chat/completions";
	private static final String API_KEY = "tgp_v1_W7T97bjAv_ipxztcF8lXKBoZ05zO8oHHJIchIRnV5to"; 
	private static final ObjectMapper mapper = new ObjectMapper();

	public static void validateUsername(String username) throws Exception {
		String prompt = buildPrompt(username);

		String requestBody = "{" + "\"model\": \"meta-llama/Llama-3.3-70B-Instruct-Turbo-Free\","
				+ "\"messages\": [{\"role\": \"user\", \"content\": \"" + prompt + "\"}]," + "\"temperature\": 0" + "}";
		System.out.println("Sending request to Together.ai with prompt:\n" + prompt);
		String response = sendPostRequest(API_URL, requestBody);
		System.out.println(response);

		// JsonNode rootNode = mapper.readTree(response);
//		String replyContent = rootNode.path("choices").get(0).path("message").path("content").asText();

	}

	private static String buildPrompt(String username) {
		return "Classify this username for spam likelihood. "
			 + "Give a score between 0 (definitely spammy) and 1 (perfectly genuine). "
			 + "Only respond with a floating point number. Username: " + username;
	}

	private static String sendPostRequest(String apiUrl, String jsonPayload) throws IOException {
		URL url = new URL(apiUrl);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setDoOutput(true);

		System.out.println("Connecting to: " + apiUrl);

		try (OutputStream os = conn.getOutputStream()) {
			byte[] input = jsonPayload.getBytes("utf-8");
			os.write(input, 0, input.length);
		}

		int status = conn.getResponseCode();
		System.out.println("HTTP Response Code: " + status);

		InputStream is = (status < 400) ? conn.getInputStream() : conn.getErrorStream();

		StringBuilder response = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"))) {
			String line;
			while ((line = br.readLine()) != null) {
				response.append(line.trim());
			}
		}

		if (status >= 400) {
			System.out.println("Error from API. Response Code: " + status);
			switch (status) {
				case 401: System.out.println("❌ Unauthorized – check your API key."); break;
				case 403: System.out.println("🚫 Forbidden – API key may not have access to this model."); break;
				case 429: System.out.println("⏳ Rate limit exceeded – slow down your requests."); break;
				case 500: System.out.println("🔥 Server error – try again later."); break;
				case 503: System.out.println("🛠️ Service unavailable – Together.ai might be down."); break;
				case 400: System.out.println("⚠️ Bad request – check your JSON format."); break;
				case 1010: System.out.println("🛑 Quota exceeded or IP blocked (Cloudflare or platform-side rate limiting)."); break;
			}
		}

		return response.toString();
	}
}
