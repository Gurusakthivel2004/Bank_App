package model;

import enums.Constants.MessageStatus;
import enums.Constants.MessageType;

public class Message extends MarkedClass {

	private Long id;
	private Long senderId;
	private MessageType messageType;
	private String messageContent;
	private MessageStatus messageStatus;
	private Long createdAt;
	private Long modifiedAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getSenderId() {
		return senderId;
	}

	public void setSenderId(Long senderId) {
		this.senderId = senderId;
	}

	public MessageType getMessageType() {
		return messageType;
	}

	public void setMessageType(MessageType messageType) {
		this.messageType = messageType;
	}

	public String getMessageContent() {
		return messageContent;
	}

	public void setMessageContent(String messageContent) {
		this.messageContent = messageContent;
	}

	public MessageStatus getMessageStatus() {
		return messageStatus;
	}

	public void setMessageStatus(MessageStatus messageStatus) {
		this.messageStatus = messageStatus;
	}

	public Long getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Long createdAt) {
		this.createdAt = createdAt;
	}

	public Long getModifiedAt() {
		return modifiedAt;
	}

	public void setModifiedAt(Long modifiedAt) {
		this.modifiedAt = modifiedAt;
	}

	@Override
	public String toString() {
		return "Message [id=" + id + ", senderId=" + senderId + ", messageType=" + messageType + ", messageContent="
				+ messageContent + ", messageStatus=" + messageStatus + ", createdAt=" + createdAt + ", modifiedAt="
				+ modifiedAt + "]";
	}
}
