package dao;

import java.util.List;
import java.util.Map;

import model.ColumnCriteria;

public interface DAO<T> {

	public List<T> get(Map<String, Object> map) throws Exception;

	public long getDataCount(Map<String, Object> txMap) throws Exception;

	public void update(ColumnCriteria columnCriteria, Map<String, Object> map) throws Exception;

	public long create(T instance) throws Exception;

}
