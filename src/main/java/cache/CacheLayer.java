package cache;

import java.util.concurrent.ConcurrentHashMap;

public class CacheLayer {
	private final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<>();

	public void put(String key, Object value) {
		cache.put(key, value);
	}

	public Object get(String key) {
		return cache.get(key);
	}

	public void invalidate(String prefix) {
		cache.keySet().removeIf(key -> key.toString().startsWith(prefix));
	}

	public boolean containsKey(String key) {
		return cache.containsKey(key);
	}
}
