package util;

import enums.Constants.HttpStatusCodes;

public class CustomException extends Exception {
	private static final long serialVersionUID = 1L;

	private final int STATUS_CODE;
	private final String STATUS_MESSAGE;

	public CustomException(String errorMessage, HttpStatusCodes status) {
		super(errorMessage);
		this.STATUS_CODE = status.getCode();
		this.STATUS_MESSAGE = status.getMessage();
	}

	public CustomException(String errorMessage, Throwable cause, HttpStatusCodes status) {
		super(errorMessage, cause);
		this.STATUS_CODE = status.getCode();
		this.STATUS_MESSAGE = status.getMessage();
	}

	public int getStatusCode() {
		return STATUS_CODE;
	}

	public String getStatusMessage() {
		return STATUS_MESSAGE;
	}

	@Override
	public String toString() {
		return String.format("CustomException[status=%d (%s), message=%s]", STATUS_CODE, STATUS_MESSAGE, getMessage());
	}
}
