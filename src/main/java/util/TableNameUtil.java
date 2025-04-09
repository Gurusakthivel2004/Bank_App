package util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import dao.DaoFactory;

public class TableNameUtil {
	private static final Map<DaoFactory.DatabaseType, Set<String>> RESERVED_TABLES = new HashMap<>();

	static {
		Set<String> postgresReserved = new HashSet<>();
		postgresReserved.add("user");

		RESERVED_TABLES.put(DaoFactory.DatabaseType.POSTGRESQL, postgresReserved);
		RESERVED_TABLES.put(DaoFactory.DatabaseType.MYSQL, new HashSet<>());
	}

	public static String getSafeTableName(String tableName) {
		Set<String> reservedWords = RESERVED_TABLES.get(DaoFactory.CURRENT_DB);
		if (reservedWords != null && reservedWords.contains(tableName.toLowerCase())) {
			return "\"" + tableName + "\"";
		}
		return tableName;
	}
}