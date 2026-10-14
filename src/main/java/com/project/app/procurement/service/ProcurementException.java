package com.project.app.procurement.service;

import org.springframework.http.HttpStatus;

public class ProcurementException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public ProcurementException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public static ProcurementException badRequest(String message) { return new ProcurementException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message); }
    public static ProcurementException conflict(String message) { return new ProcurementException(HttpStatus.CONFLICT, "CONFLICT", message); }
    public static ProcurementException notFound() { return new ProcurementException(HttpStatus.NOT_FOUND, "NOT_FOUND", "The requested resource was not found."); }
}
