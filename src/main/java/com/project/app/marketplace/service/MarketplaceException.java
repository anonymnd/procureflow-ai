package com.project.app.marketplace.service;

import org.springframework.http.HttpStatus;

public class MarketplaceException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public MarketplaceException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public static MarketplaceException badRequest(String message) { return new MarketplaceException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message); }
    public static MarketplaceException conflict(String message) { return new MarketplaceException(HttpStatus.CONFLICT, "CONFLICT", message); }
    public static MarketplaceException notFound() { return new MarketplaceException(HttpStatus.NOT_FOUND, "NOT_FOUND", "The requested resource was not found."); }
}
