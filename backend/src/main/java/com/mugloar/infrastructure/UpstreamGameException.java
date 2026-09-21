package com.mugloar.infrastructure;

public class UpstreamGameException extends RuntimeException {
    public UpstreamGameException(String message, Throwable cause) {
        super(message, cause);
    }
}
