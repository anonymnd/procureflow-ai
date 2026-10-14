package com.project.app.common.web;

import java.time.Instant;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import com.project.app.common.exception.ResourceNotFoundException;

/**
 * App-wide fallback advice (moved from {@code supplier.controller} by REQ-S2-005). Explicitly
 * ordered last: it is unscoped (applies to every controller), so it must never win a catch-all
 * race against a package-scoped advice bean (see {@code identity.api.IdentityExceptionHandler}
 * and {@code marketplace.api.MarketplaceExceptionHandler}, both pinned ahead of it).
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .allow(ex.getSupportedHttpMethods() == null ? new org.springframework.http.HttpMethod[0]
                        : ex.getSupportedHttpMethods().toArray(org.springframework.http.HttpMethod[]::new))
                .body(new ApiError(Instant.now(), 405, "METHOD_NOT_ALLOWED", "This method is not supported."));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFoundException(ResourceNotFoundException ex) {

        ApiError apiError = new ApiError(
                Instant.now(),
                404,
                "Resource Not Found",
                ex.getMessage());

        return ResponseEntity.status(404).body(apiError);

    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " +
                        error.getDefaultMessage())
                .findFirst()
                .orElse("Invalid request");

        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

    }

    /** Any authenticated request to a route with no controller mapping (e.g. the removed legacy `/api/suppliers/**`, REQ-S2-005) falls through to the static-resource handler, which throws this; without this handler it would be swallowed by {@link #handleUnexpectedException} as a false 500. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(NoResourceFoundException ex) {
        ApiError error = new ApiError(Instant.now(), 404, "Resource Not Found", "The requested resource was not found.");
        return ResponseEntity.status(404).body(error);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> handleBadRequest(Exception ex) {
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                "Request body or path parameter is invalid");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException ex) {

        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                "DATA_CONFLICT",
                "The operation conflicts with existing data");

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedException(
            Exception ex) {

        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
}
