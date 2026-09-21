package com.mugloar.infrastructure;

public class UpstreamGameException extends RuntimeException {
    private final int statusCode;

    public UpstreamGameException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int statusCode() { return statusCode; }
}
