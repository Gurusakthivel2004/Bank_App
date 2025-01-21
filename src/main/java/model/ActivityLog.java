package model;

import Enum.Constants.LogType;

public class ActivityLog extends MarkedClass {

	private Long id;
	private String tableName;
	private LogType logType;
	private Long rowId;
	private Long userId;
	private Long userAccountNumber;
	private String logMessage;
	private Long timestamp;
	private Long performedBy;

	public ActivityLog() {
	}

	public Long getId() {
		return id;
	}

	public ActivityLog setId(Long id) {
		this.id = id;
		return this;
	}

	public String getTableName() {
		return tableName;
	}

	public ActivityLog setTableName(String tableName) {
		this.tableName = tableName;
		return this;
	}

	public LogType getLogType() {
		return logType;
	}

	public ActivityLog setLogType(LogType logType) {
		this.logType = logType;
		return this;
	}

	public Long getRowId() {
		return rowId;
	}

	public ActivityLog setRowId(Long rowId) {
		this.rowId = rowId;
		return this;
	}

	public Long getUserId() {
		return userId;
	}

	public ActivityLog setUserId(Long userId) {
		this.userId = userId;
		return this;
	}

	public Long getUserAccountNumber() {
		return userAccountNumber;
	}

	public ActivityLog setUserAccountNumber(Long userAccountNumber) {
		this.userAccountNumber = userAccountNumber;
		return this;
	}

	public String getLogMessage() {
		return logMessage;
	}

	public ActivityLog setLogMessage(String logMessage) {
		this.logMessage = logMessage;
		return this;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public ActivityLog setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
		return this;
	}

	public Long getPerformedBy() {
		return performedBy;
	}

	public ActivityLog setPerformedBy(Long performedBy) {
		this.performedBy = performedBy;
		return this;
	}

	@Override
	public String toString() {
		return "ActivityLog [id=" + id + ", tableName=" + tableName + ", logType=" + logType + ", rowId=" + rowId
				+ ", userId=" + userId + ", userAccountNumber=" + userAccountNumber + ", logMessage=" + logMessage
				+ ", timestamp=" + timestamp + ", performedBy=" + performedBy + "]";
	}

}
