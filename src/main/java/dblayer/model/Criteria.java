package dblayer.model;

import java.util.ArrayList;
import java.util.List;

public class Criteria {
	
	@Override
	public String toString() {
		return "Criteria [clazz=" + clazz + ", tableName=" + tableName + ", selectColumn=" + selectColumn + ", column="
				+ column + ", value=" + value + ", operator=" + operator + ", logicalOperator=" + logicalOperator
				+ ", values=" + values + ", joinColumn=" + joinColumn + ", joinOperator=" + joinOperator
				+ ", joinValue=" + joinValue + ", joinTable=" + joinTable + ", orderBy=" + orderBy + ", orderByField="
				+ orderByField + ", limitValue=" + limitValue + "]";
	}


	private Class<? extends MarkedClass> clazz;
	private String tableName;
	private List<String> selectColumn;
	private List<String> column;
    private List<Object> value;
    private List<String> operator;
    private String logicalOperator;
    // values is for BETWEEN IN operations..
    private List<Object> values;
	private List<String> joinColumn;
	private List<String> joinOperator;
	private List<String> joinValue;
    private List<Object> joinTable;
    private String orderBy;
    private String orderByField;
    private Object limitValue;
    
    public Criteria() {
    	column = new ArrayList<String>();
    	value = new ArrayList<Object>();
    	operator = new ArrayList<String>();
    }
    
    
    public String getTableName() {
        return tableName;
    }
    
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
    
    public List<String> getColumn() {
        return column;
    }

    public void setColumn(List<String> column) {
        this.column = column;
    }

    public List<Object> getValue() {
        return value;
    }

    public void setValue(List<Object> value) {
        this.value = value;
    }

    public List<Object> getValues() {
        return values;
    }

    public void setValues(List<Object> values) {
        this.values = values;
    }

    public List<String> getOperator() {
        return operator;
    }

    public void setOperator(List<String> operator) {
        this.operator = operator;
    }
    
    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

	public List<String> getSelectColumn() {
		return selectColumn;
	}

	public void setSelectColumn(List<String> selectColumn) {
		this.selectColumn = selectColumn;
	}

	public Class<?> getClazz() {
		return clazz;
	}

	public void setClazz(Class<? extends MarkedClass> clazz) {
		this.clazz = clazz;
	}

	public Object getLimitValue() {
		return limitValue;
	}

	public void setLimitValue(Object limitValue) {
		this.limitValue = limitValue;
	}

	public String getLogicalOperator() {
		return logicalOperator;
	}

	public void setLogicalOperator(String logicalOperator) {
		this.logicalOperator = logicalOperator;
	}

	public List<String> getJoinColumn() {
		return joinColumn;
	}

	public void setJoinColumn(List<String> joinColumn) {
		this.joinColumn = joinColumn;
	}

	public List<String> getJoinValue() {
		return joinValue;
	}

	public void setJoinValue(List<String> joinValue) {
		this.joinValue = joinValue;
	}

	public List<Object> getJoinTable() {
		return joinTable;
	}

	public void setJoinTable(List<Object> joinTable) {
		this.joinTable = joinTable;
	}

	public List<String> getJoinOperator() {
		return joinOperator;
	}

	public void setJoinOperator(List<String> joinOperator) {
		this.joinOperator = joinOperator;
	}


	public String getOrderByField() {
		return orderByField;
	}


	public void setOrderByField(String orderByField) {
		this.orderByField = orderByField;
	}
}
