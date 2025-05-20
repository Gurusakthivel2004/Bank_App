package model;

public class FailedRequest extends MarkedClass {

    private Long id;
    private String requestJson;
    private Integer retries;
    private Long createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestJson() {
		return requestJson;
	}

	public void setRequestJson(String requestJson) {
		this.requestJson = requestJson;
	}

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

	@Override
	public String toString() {
		return "FailedRequest [id=" + id + ", requestJson=" + requestJson + ", createdAt=" + createdAt + "]";
	}

	public Integer getRetries() {
		return retries;
	}

	public void setRetries(Integer retries) {
		this.retries = retries;
	}
    
}