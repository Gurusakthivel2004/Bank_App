package dao;

import java.util.List;
import java.util.Map;

import model.ColumnCriteria;
import util.CustomException;

public interface DAO<T> {

	public List<T> get(Map<String, Object> map) throws CustomException;

	public Long getDataCount(Map<String, Object> txMap) throws CustomException;

	public void update(ColumnCriteria columnCriteria, Map<String, Object> map) throws CustomException;

	public Long create(T instance) throws CustomException;

}
