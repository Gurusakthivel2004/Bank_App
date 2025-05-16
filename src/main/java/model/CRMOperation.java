package model;

import java.util.Map;

public class CRMOperation {
    private Map<Object, Object> data;
    private String endpoint;

    public CRMOperation(Map<Object, Object> data, String endpoint) {
        this.data = data;
        this.endpoint = endpoint;
    }

    public Map<Object, Object> getData() {
        return data;
    }

    public String getEndpoint() {
        return endpoint;
    }
}
