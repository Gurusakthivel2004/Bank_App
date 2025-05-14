package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.Base64;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HttpUtil {
	
	private static final Logger LOGGER = LogManager.getLogger(HttpUtil.class);
	private static final String DEFAULT_CONTENT_TYPE = "application/x-www-form-urlencoded";
	
	public static String sendPostRequest(String url, String jsonBody, String bearerToken) throws Exception {
		return sendRequest(url, "POST", jsonBody, bearerToken, null, null, false, null);
	}
	
	public static String sendPostRequest(String url, String params) throws Exception {
		return sendRequest(url, "POST", params, null, null, null, false, DEFAULT_CONTENT_TYPE);
	}

	public static String sendPostRequestProxy(String url, String jsonBody, String bearerToken,
			String basicUsername, String basicPassword) throws Exception {
		return sendRequest(url, "POST", jsonBody, bearerToken, basicUsername, basicPassword, true, null);
	}

	public static String sendPutRequestProxy(String url, String jsonBody, String bearerToken,
			String basicUsername, String basicPassword) throws Exception {
		return sendRequest(url, "PUT", jsonBody, bearerToken, basicUsername, basicPassword, true, null);
	}

	public static String sendGetRequest(String url, String bearerToken) throws Exception {
		return sendRequest(url, "GET", null, bearerToken, null, null, false, null);
	}

	public static String sendGetRequestProxy(String url, String bearerToken, String basicUsername,
			String basicPassword) throws Exception {
		return sendRequest(url, "GET", null, bearerToken, basicUsername, basicPassword, true, null);
	}
	
	private static String readStream(InputStream stream) throws Exception {
		BufferedReader in = new BufferedReader(new InputStreamReader(stream));
		StringBuilder response = new StringBuilder();
		String inputLine;
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		return response.toString();
	}
	
	private static String sendRequest(String url, String method, String body, String bearerToken, String basicUsername,
			String basicPassword, boolean useProxy, String contentType) throws Exception {

		HttpURLConnection conn;

		LOGGER.info("---- Sending HTTP Request ----");
		LOGGER.info("URL           : " + url);
		LOGGER.info("Method        : " + method);
		LOGGER.info("Using Proxy   : " + useProxy);
		LOGGER.info("Content-Type  : " + (contentType != null ? contentType : "application/json"));
		LOGGER.info("Body   : " + body);

		if (useProxy) {
			String proxyHost = OAuthConfig.get("proxy.host");
			int proxyPort = Integer.parseInt(OAuthConfig.get("proxy.port"));
			LOGGER.info("Proxy Host    : " + proxyHost);
			LOGGER.info("Proxy Port    : " + proxyPort);
			Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
			conn = (HttpURLConnection) new URL(url).openConnection(proxy);
		} else {
			conn = (HttpURLConnection) new URL(url).openConnection();
		}

		conn.setRequestMethod(method);
		conn.setConnectTimeout(10000);
		conn.setReadTimeout(10000);

		if (body != null && !body.isEmpty()) {
			conn.setDoOutput(true);
			conn.setRequestProperty("Content-Type", contentType != null ? contentType : "application/json");
		}

		if (bearerToken != null && !bearerToken.isEmpty()) {
			conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
			LOGGER.info("Authorization : Bearer Token Set");
		} else if (basicUsername != null && basicPassword != null) {
			String basicToken = Base64.getEncoder()
					.encodeToString((basicUsername + ":" + basicPassword).getBytes("UTF-8"));
			conn.setRequestProperty("Authorization", "Basic " + basicToken);
			LOGGER.info("Authorization : Basic Auth Set");
		}

		if (body != null && !body.isEmpty()) {
			try (OutputStream os = conn.getOutputStream()) {
				byte[] input = body.getBytes("UTF-8");
				os.write(input, 0, input.length);
				LOGGER.info("Request Body  : " + body);
			}
		}

		int responseCode = conn.getResponseCode();
		LOGGER.info("Response Code : " + responseCode);

		if (responseCode >= 200 && responseCode < 300) {
			String response = "";
			try {
				response = readStream(conn.getInputStream());
			} catch (IOException e) {
				InputStream errorStream = conn.getErrorStream();
				response = errorStream != null ? readStream(errorStream) : "No response body";
			}
			LOGGER.info("Full Response Body: " + response);
			return response;
		} else {
			InputStream errorStream = conn.getErrorStream();
			String errorMessage = errorStream != null ? readStream(errorStream) : "Error: No response body";
			LOGGER.error("HTTP error code: " + responseCode + " - " + errorMessage);
			throw new IOException("HTTP error code: " + responseCode + " - " + errorMessage);
		}
	}
	
}
