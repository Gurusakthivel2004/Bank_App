package util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.yaml.snakeyaml.Yaml;

public class ColumnYamlUtil {
	private static final String FILE_PATH = "/home/guru-pt7672/git/BankApplication/Bank_Application/src/main/ColumnMappings.yaml";

	private static class Holder {
		private static final Map<String, ClassMapping> INSTANCE = loadMappings();
	}

	private ColumnYamlUtil() {}

	@SuppressWarnings("unchecked")
	private static Map<String, ClassMapping> loadMappings() {
		try (InputStream inputStream = new FileInputStream(FILE_PATH)) {
			Yaml yaml = new Yaml();
			Map<String, Map<String, Object>> yamlData = yaml.load(inputStream);
			Map<String, Object> tableData = (Map<String, Object>) yamlData.get("classes");

			Map<String, ClassMapping> mappings = new ConcurrentHashMap<>();
			for (Map.Entry<String, Object> entry : tableData.entrySet()) {
				String tableName = entry.getKey();
				Map<String, Object> tableMappingData = (Map<String, Object>) entry.getValue();

				ClassMapping tableMapping = new ClassMapping();
				tableMapping.setTableName((String) tableMappingData.get("tableName"));
				tableMapping.setAutoIncrementField((String) tableMappingData.get("autoIncrementField"));
				tableMapping.setReferenceField((String) tableMappingData.get("referenceField"));
				tableMapping.setReferedField((String) tableMappingData.get("referedField"));

				Map<String, FieldMapping> fields = new HashMap<>();
				Map<String, Object> fieldsData = (Map<String, Object>) tableMappingData.get("fields");

				if (fieldsData != null) {
					for (Map.Entry<String, Object> fieldEntry : fieldsData.entrySet()) {
						String columnName = fieldEntry.getKey();
						Map<String, Object> fieldMappingData = (Map<String, Object>) fieldEntry.getValue();

						FieldMapping fieldMapping = new FieldMapping();
						fieldMapping.setColumnName((String) fieldMappingData.get("columnName"));
						fieldMapping.setType((String) fieldMappingData.get("type"));

						fields.put(columnName, fieldMapping);
					}
				}

				tableMapping.setFields(fields);
				mappings.put(tableName, tableMapping);
			}
			return mappings;
		} catch (IOException e) {
			throw new RuntimeException("Failed to load YAML mappings", e);
		}
	}

	public static ClassMapping getMapping(String className) {
		ClassMapping mapping = Holder.INSTANCE.get(className);
		if (mapping != null) {
			return mapping;
		} else {
			throw new IllegalArgumentException("Mapping for table '" + className + "' not found in YAML.");
		}
	}

	public static class ClassMapping {
		private String tableName;
		private String referenceField;
		private String referedField;
		private String autoIncrementField;
		private Map<String, FieldMapping> fields;

		public String getTableName() {
			return tableName;
		}

		public void setTableName(String tableName) {
			this.tableName = tableName;
		}

		public Map<String, FieldMapping> getFields() {
			return fields;
		}

		public void setFields(Map<String, FieldMapping> fields) {
			this.fields = fields;
		}

		public String getAutoIncrementField() {
			return autoIncrementField;
		}

		public void setAutoIncrementField(String autoIncrementField) {
			this.autoIncrementField = autoIncrementField;
		}

		public String getReferenceField() {
			return referenceField;
		}

		public void setReferenceField(String referenceField) {
			this.referenceField = referenceField;
		}

		public String getReferedField() {
			return referedField;
		}

		public void setReferedField(String referedField) {
			this.referedField = referedField;
		}
	}

	public static class FieldMapping {
		private String columnName;
		private String type;

		public String getColumnName() {
			return columnName;
		}

		public void setColumnName(String columnName) {
			this.columnName = columnName;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}
	}
}
