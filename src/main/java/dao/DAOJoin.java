package dao;

import java.util.List;
import java.util.Map;

import model.JoinObject;
import util.CustomException;

public interface DAOJoin<T> {

	public List<JoinObject<T>> getJoined(Map<String, Object> map) throws CustomException;

}
