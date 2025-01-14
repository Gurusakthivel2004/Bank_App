package util;

import Enum.Constants.HttpStatusCodes;

public class CustomException extends Exception {
	private static final long serialVersionUID = 1L;

	private final int statusCode;
	private final String statusMessage;

	public CustomException(String errorMessage, HttpStatusCodes status) {
		super(errorMessage);
		this.statusCode = status.getCode();
		this.statusMessage = status.getMessage();
	}

	public CustomException(String errorMessage, Throwable cause, HttpStatusCodes status) {
		super(errorMessage, cause);
		this.statusCode = status.getCode();
		this.statusMessage = status.getMessage();
	}

	public int getStatusCode() {
		return statusCode;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	@Override
	public String toString() {
		return String.format("CustomException[status=%d (%s), message=%s]", statusCode, statusMessage, getMessage());
	}
}
