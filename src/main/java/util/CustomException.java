package util;

public class CustomException extends Exception {  
    public CustomException(String errorMessage) {  
      super(errorMessage);  
    }  
    public CustomException(String message, Throwable cause) {
        super(message, cause);
    }
} 
