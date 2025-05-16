package util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class OAuthConfig {

	private static final String FILE = "oauth_config.properties";
	private static Properties properties = new Properties();

	static {

		try (InputStream input = OAuthConfig.class.getClassLoader().getResourceAsStream(FILE)) {
			if (input == null) {
				throw new RuntimeException("Properties file not found in classpath!");
			}
			properties.load(input);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to load OAuth configuration", e);
		}
	}

	public static String get(String key) {
		return properties.getProperty(key);
	}
}