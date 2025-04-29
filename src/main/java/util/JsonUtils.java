package util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import enums.Constants.SymbolProvider;

public class JsonUtils {

	private static final Logger logger = LogManager.getLogger(JsonUtils.class);
	private static final ObjectMapper mapper = new ObjectMapper();

	public static String getValueByPath(String jsonString, String path, String key) throws Exception {
		JsonNode rootNode = mapper.readTree(jsonString);

		JsonNode targetNode = getNodeByPath(rootNode, path);

		if (targetNode != null && targetNode.has(key)) {
			return targetNode.get(key).asText();
		}

		return null;
	}

	private static JsonNode getNodeByPath(JsonNode node, String path) {
		String[] parts = path.split("\\.");

		for (String part : parts) {
			if (part.matches(".*\\[\\d+]$")) {
				String arrayName = part.substring(0, part.indexOf('['));
				int index = Integer.parseInt(part.replaceAll("[^\\d]", ""));

				node = node.path(arrayName);
				if (node.isArray() && node.size() > index) {
					node = node.get(index);
				} else {
					return null;
				}
			} else {
				node = node.path(part);
			}

			if (node.isMissingNode())
				return null;
		}

		return node;
	}

	public static <K extends SymbolProvider> String buildModuleJsonFromMap(Map<K, Object> dataMap) {
		JsonObject obj = new JsonObject();

		List<String> specialKeys = Arrays.asList("FK_User_Id", "FK_Account_Name", "FK_Contact_Name");

		Map<String, Object> stringKeyedMap = new HashMap<>();
		for (Map.Entry<K, Object> entry : dataMap.entrySet()) {
			String keyStr;
			SymbolProvider key = entry.getKey();

			keyStr = key.getSymbol();
			stringKeyedMap.put(keyStr, entry.getValue());
		}

		logger.info(stringKeyedMap.keySet());
		logger.info(stringKeyedMap.values());

		if (stringKeyedMap.containsKey("FK_User_Id")) {
			JsonObject ownerObj = new JsonObject();
			ownerObj.addProperty("id", stringKeyedMap.get("FK_User_Id").toString());
			obj.add("Owner", ownerObj);
		}

		if (stringKeyedMap.containsKey("FK_Account_Name")) {
			JsonObject accountObj = new JsonObject();
			accountObj.addProperty("id", stringKeyedMap.get("FK_Account_Name").toString());
			obj.add("Account_Name", accountObj);
		}
		
		if (stringKeyedMap.containsKey("FK_Contact_Name")) {
			JsonObject contactObj = new JsonObject();
			contactObj.addProperty("id", stringKeyedMap.get("FK_Contact_Name").toString());
			obj.add("Contact_Name", contactObj);
		}


		for (Map.Entry<String, Object> entry : stringKeyedMap.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			if (specialKeys.contains(key)) {
				continue;
			}

			if (value instanceof Number) {
				obj.addProperty(key, (Number) value);
			} else if (value instanceof Boolean) {
				obj.addProperty(key, (Boolean) value);
			} else {
				obj.addProperty(key, value != null ? value.toString() : null);
			}
		}
		JsonArray dataArray = new JsonArray();
		dataArray.add(obj);
		JsonObject root = new JsonObject();
		root.add("data", dataArray);

		return root.toString();
	}

}
