package util;

public class CustomException extends Exception {
	private static final long serialVersionUID = 1L;

	public CustomException(String errorMessage) {
		super(errorMessage);
	}

	public CustomException(String message, Throwable cause) {
		super(message, cause);
	}
}
