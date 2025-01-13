package model;

import java.util.HashMap;
import java.util.Map;

public class JoinObject {
	private Object instance;
	private Map<String, Object> joinedFields;

	public JoinObject(Object instance) {
		this.instance = instance;
		this.joinedFields = new HashMap<>();
	}

	public Object getInstance() {
		return instance;
	}

	public Map<String, Object> getjoinedFields() {
		return joinedFields;
	}

	public void addjoinedField(String key, Object value) {
		joinedFields.put(key, value);
	}
}
