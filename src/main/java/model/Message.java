package model;

import Enum.Constants.LogType;

public class Message {
	private Long id;
	private Long senderId;
	private Long recieverId;
	private MessageType messageType;
	private String messageContent;
	private MessageStatus messageStatus;
	private Long createdAt;
	private Long updatedAt;
}
