package model;

import java.util.List;

public class ColumnCriteria {

	// fields: fields of the pojo class to be updated.
	// values: values of the field.

	@Override
	public String toString() {
		return "ColumnCriteria [fields=" + fields + ", values=" + values + "]";
	}

	private List<String> fields;
	private List<Object> values;

	public List<String> getFields() {
		return fields;
	}

	public ColumnCriteria setFields(List<String> field) {
		this.fields = field;
		return this;
	}

	public List<Object> getValues() {
		return values;
	}

	public ColumnCriteria setValues(List<Object> values) {
		this.values = values;
		return this;
	}

}
