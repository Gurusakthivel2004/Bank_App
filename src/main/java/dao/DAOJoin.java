package dao;

import java.util.List;
import java.util.Map;

import model.JoinObject;

public interface DAOJoin<T> {

	public List<JoinObject<T>> getJoined(Map<String, Object> map) throws Exception;

}
