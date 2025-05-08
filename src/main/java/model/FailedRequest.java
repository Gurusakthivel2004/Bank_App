package model;

import enums.Constants.RetryStatus;

public class FailedRequest extends MarkedClass {

    private Long id;
    private String url;
    private String method;
    private String requestBody;
    private Integer statusCode;
    private String errorMessage;
    private Long createdAt;
    private RetryStatus retryStatus;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public RetryStatus getRetryStatus() {
        return retryStatus;
    }

    public void setRetryStatus(RetryStatus retryStatus) {
        this.retryStatus = retryStatus;
    }

    @Override
    public String toString() {
        return "FailedRequest{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", method='" + method + '\'' +
                ", requestBody='" + requestBody + '\'' +
                ", statusCode=" + statusCode +
                ", errorMessage='" + errorMessage + '\'' +
                ", createdAt=" + createdAt +
                ", retryStatus=" + retryStatus +
                '}';
    }
}
