package com.project.app.marketplace.api;

import java.util.Map;

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

import com.project.app.marketplace.service.MarketplaceException;

/**
 * Ordered ahead of the unscoped legacy supplier {@code GlobalExceptionHandler} so
 * {@link DataIntegrityViolationException} from a marketplace controller (the duplicate-profile
 * race) gets this package's {code,message} body shape, not that handler's {timestamp,status,error,message}.
 */
@RestControllerAdvice(basePackages = "com.project.app.marketplace")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MarketplaceExceptionHandler {
    @ExceptionHandler(MarketplaceException.class)
    ResponseEntity<Map<String, String>> marketplaceFailure(MarketplaceException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(Map.of("code", exception.getCode(), "message", exception.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, String>> conflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("code", "CONFLICT", "message", "The operation conflicts with existing data."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> invalidRequest(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream().findFirst()
                .map(error -> error.getField() + " is invalid.").orElse("Request is invalid.");
        return ResponseEntity.badRequest().body(Map.of("code", "VALIDATION_ERROR", "message", message));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    ResponseEntity<Map<String, String>> badRequest() {
        return ResponseEntity.badRequest()
                .body(Map.of("code", "BAD_REQUEST", "message", "Request body or path parameter is invalid."));
    }
}
