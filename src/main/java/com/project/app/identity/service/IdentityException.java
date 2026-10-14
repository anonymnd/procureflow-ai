package com.project.app.identity.service;

import org.springframework.http.HttpStatus;

public class IdentityException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public IdentityException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public static IdentityException badRequest(String message) { return new IdentityException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message); }
    public static IdentityException conflict(String message) { return new IdentityException(HttpStatus.CONFLICT, "CONFLICT", message); }
    public static IdentityException notFound() { return new IdentityException(HttpStatus.NOT_FOUND, "NOT_FOUND", "The requested resource was not found."); }
    public static IdentityException unavailable() { return new IdentityException(HttpStatus.SERVICE_UNAVAILABLE, "DELIVERY_UNAVAILABLE", "Email delivery is not configured. Contact the service administrator."); }
}
