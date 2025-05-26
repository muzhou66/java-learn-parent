package com.muzhou.learn.common.exception;

/**
 * Custom business exception for MuZhou application
 */
public class MuZhouException extends RuntimeException {
    // Error code (can be used for i18n or specific error handling)
    private final String errorCode;
    
    // Additional error data (optional)
    private final Object errorData;

    // Constructors
    
    public MuZhouException(String message) {
        super(message);
        this.errorCode = "DEFAULT_ERROR";
        this.errorData = null;
    }

    public MuZhouException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.errorData = null;
    }

    public MuZhouException(String errorCode, String message, Object errorData) {
        super(message);
        this.errorCode = errorCode;
        this.errorData = errorData;
    }

    public MuZhouException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorData = null;
    }

    public MuZhouException(String errorCode, String message, Object errorData, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorData = errorData;
    }

    // Getters
    public String getErrorCode() {
        return errorCode;
    }

    public Object getErrorData() {
        return errorData;
    }

    @Override
    public String toString() {
        return "MuZhouException{" +
                "errorCode='" + errorCode + '\'' +
                ", message='" + getMessage() + '\'' +
                ", errorData=" + errorData +
                '}';
    }
}