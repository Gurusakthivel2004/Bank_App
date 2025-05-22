package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class UsernameValidator {

	private static final Logger LOGGER = LogManager.getLogger(UsernameValidator.class);

	private static final String API_URL = "https://api.together.xyz/v1/chat/completions";
	private static final String API_KEY = "tgp_v1_LUOus5An8irdEZr7o9EQOmKW5LwkOOR_4_iM3j132KM";
	private static final String MODEL = "meta-llama/Llama-Vision-Free";

	private static String buildPrompt(String username) {
		return "Classify this username for spam likelihood. "
				+ "Give a score between 1 (definitely spammy) and 10 (perfectly genuine). "
				+ "Only respond with a whole number. Username: " + username;
	}
	
	public static Integer validateUsername(String username) throws IOException {
		String response = getScore(username);
		Integer score = extractScore(response);
		return score;
	}

	private static Integer extractScore(String jsonResponse) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(jsonResponse);

			JsonNode choices = root.path("choices");
			if (choices.isArray() && choices.size() > 0) {
				String content = choices.get(0).path("message").path("content").asText();
				return Integer.parseInt(content.trim());
			}
		} catch (Exception e) {
			LOGGER.error("Failed to extract score: " + e.getMessage());
		}
		return null;
	}

	private static String getScore(String username) throws IOException {
		URL url = new URL(API_URL);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
		conn.setRequestProperty("User-Agent", "Mozilla/5.0");
		conn.setRequestProperty("Accept", "*/*");
		conn.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
		conn.setRequestProperty("Connection", "keep-alive");

		ObjectMapper mapper = new ObjectMapper();

		String prompt = buildPrompt(username);
		ObjectNode root = mapper.createObjectNode();
		root.put("model", MODEL);

		ArrayNode messages = root.putArray("messages");

		ObjectNode message = mapper.createObjectNode();
		message.put("role", "user");
		message.put("content", prompt);
		messages.add(message);

		String requestBody = mapper.writeValueAsString(root);

		try (OutputStream os = conn.getOutputStream()) {
			os.write(requestBody.getBytes(StandardCharsets.UTF_8));
		}

		int responseCode = conn.getResponseCode();
		LOGGER.info("HTTP response code: " + responseCode);

		InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();

		String encoding = conn.getContentEncoding();
		return getResponse(is, encoding);
	}

	private static String getResponse(InputStream is, String encoding) throws IOException {
		if ("gzip".equalsIgnoreCase(encoding)) {
			is = new GZIPInputStream(is);
		}

		StringBuilder responseBuilder = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				responseBuilder.append(line);
			}
		}
		return responseBuilder.toString();
	}

}
