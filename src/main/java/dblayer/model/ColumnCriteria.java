package dblayer.model;

import java.util.List;

public class ColumnCriteria {
	
	// fields: fields of the pojo class to be updated.
	// values: values of the field.
	
    private List<String> fields; 
    private List<Object> values; 
    
	public List<String> getFields() {
		return fields;
	}
	public void setFields(List<String> field) {
		this.fields = field;
	}
	public List<Object> getValues() {
		return values;
	}
	public void setValues(List<Object> values) {
		this.values = values;
	}

}
