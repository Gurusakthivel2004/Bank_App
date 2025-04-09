package model;

import java.util.HashMap;
import java.util.Map;

public class JoinObject<T> {
	private T instance;
	private Map<String, Object> joinedFields;

	public JoinObject(T instance) {
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

	@Override
	public String toString() {
		return "JoinObject [instance=" + instance + ", joinedFields=" + joinedFields + "]";
	}

}
