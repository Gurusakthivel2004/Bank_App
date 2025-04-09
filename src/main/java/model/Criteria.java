package model;

import java.util.ArrayList;
import java.util.List;

public class Criteria {

	private Class<?> clazz;
	private String tableName;
	private List<String> selectColumn;
	private List<String> column;
	private List<Object> value;
	private List<String> operator;
	private String logicalOperator;
	private String aggregateFunction;
	private String aggregateOperator;
	// values is for BETWEEN IN operations..
	private List<Object> values;
	private String join;
	private List<String> joinColumn;
	private List<String> joinOperator;
	private List<Object> joinValue;
	private List<String> joinTable;
	private String orderBy;
	private List<String> Alias;
	private String orderByField;
	private Object limitValue;
	private Long offsetValue;

	public Criteria() {
		column = new ArrayList<String>();
		value = new ArrayList<Object>();
		operator = new ArrayList<String>();
	}

	public String getTableName() {
		return tableName;
	}

	public Criteria setTableName(String tableName) {
		this.tableName = tableName;
		return this;
	}

	public List<String> getColumn() {
		return column;
	}

	public Criteria setColumn(List<String> column) {
		this.column = column;
		return this;
	}

	public List<Object> getValue() {
		return value;
	}

	public Criteria setValue(List<Object> value) {
		this.value = value;
		return this;
	}

	public List<Object> getValues() {
		return values;
	}

	public Criteria setValues(List<Object> values) {
		this.values = values;
		return this;
	}

	public List<String> getOperator() {
		return operator;
	}

	public Criteria setOperator(List<String> operator) {
		this.operator = operator;
		return this;
	}

	public String getOrderBy() {
		return orderBy;
	}

	public Criteria setOrderBy(String orderBy) {
		this.orderBy = orderBy;
		return this;
	}

	public List<String> getSelectColumn() {
		return selectColumn;
	}

	public Criteria setSelectColumn(List<String> selectColumn) {
		this.selectColumn = selectColumn;
		return this;
	}

	public Class<?> getClazz() {
		return clazz;
	}

	@SuppressWarnings("unchecked")
	public <T> Criteria setClazz(Class<T> clazz) {
		if (MarkedClass.class.isAssignableFrom(clazz)) {
			this.clazz = (Class<? extends MarkedClass>) clazz;
		} else if (clazz == Integer.class) {
			this.clazz = (Class<? extends MarkedClass>) clazz;
		} else {
			throw new IllegalArgumentException("Invalid class type: " + clazz.getName());
		}
		return this;
	}

	public Object getLimitValue() {
		return limitValue;
	}

	public Criteria setLimitValue(Object limitValue) {
		this.limitValue = limitValue;
		return this;
	}

	public String getLogicalOperator() {
		return logicalOperator;
	}

	public Criteria setLogicalOperator(String logicalOperator) {
		this.logicalOperator = logicalOperator;
		return this;
	}

	public List<String> getJoinColumn() {
		return joinColumn;
	}

	public Criteria setJoinColumn(List<String> joinColumn) {
		this.joinColumn = joinColumn;
		return this;
	}

	public List<Object> getJoinValue() {
		return joinValue;
	}

	public Criteria setJoinValue(List<Object> joinValue) {
		this.joinValue = joinValue;
		return this;
	}

	public List<String> getJoinTable() {
		return joinTable;
	}

	public Criteria setJoinTable(List<String> joinTable) {
		this.joinTable = joinTable;
		return this;
	}

	public List<String> getJoinOperator() {
		return joinOperator;
	}

	public Criteria setJoinOperator(List<String> joinOperator) {
		this.joinOperator = joinOperator;
		return this;
	}

	public String getOrderByField() {
		return orderByField;
	}

	public Criteria setOrderByField(String orderByField) {
		this.orderByField = orderByField;
		return this;
	}

	public Long getOffsetValue() {
		return offsetValue;
	}

	public Criteria setOffsetValue(Long offsetValue) {
		this.offsetValue = offsetValue;
		return this;
	}

	public List<String> getAlias() {
		return Alias;
	}

	public Criteria setAlias(List<String> alias) {
		Alias = alias;
		return this;
	}

	public String getJoin() {
		return join;
	}

	public Criteria setJoin(String join) {
		this.join = join;
		return this;
	}

	public String getAggregateFunction() {
		return aggregateFunction;
	}

	public Criteria setAggregateFunction(String aggregateFunction) {
		this.aggregateFunction = aggregateFunction;
		return this;
	}

	public String getAggregateOperator() {
		return aggregateOperator;
	}

	public Criteria setAggregateOperator(String aggregateOperator) {
		this.aggregateOperator = aggregateOperator;
		return this;
	}

	@Override
	public String toString() {
		return "Criteria [clazz=" + clazz + ", tableName=" + tableName + ", selectColumn=" + selectColumn + ", column="
				+ column + ", value=" + value + ", operator=" + operator + ", logicalOperator=" + logicalOperator
				+ ", values=" + values + ", joinColumn=" + joinColumn + ", joinOperator=" + joinOperator
				+ ", joinValue=" + joinValue + ", joinTable=" + joinTable + ", orderBy=" + orderBy + ", orderByField="
				+ orderByField + ", limitValue=" + limitValue + ", offsetValue=" + offsetValue + "]";
	}
}