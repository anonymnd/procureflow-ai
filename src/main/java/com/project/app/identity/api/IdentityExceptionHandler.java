package com.project.app.identity.api;

import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.project.app.identity.service.IdentityException;

/**
 * Explicitly ordered ahead of the unscoped {@code common.web.GlobalExceptionHandler}. Without
 * this, relative precedence between two otherwise-unordered advice beans falls back to scan
 * order, which silently flips if either class ever moves to a different package (it did: moving
 * {@code GlobalExceptionHandler} from {@code supplier} to {@code common} made it win the
 * catch-all race here, turning every {@link IdentityException} into a false 500).
 */
@RestControllerAdvice(basePackages = "com.project.app.identity")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IdentityExceptionHandler {
    @ExceptionHandler(IdentityException.class)
    ResponseEntity<Map<String, String>> identityFailure(IdentityException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(Map.of("code", exception.getCode(), "message", exception.getMessage()));
    }

    @ExceptionHandler({AuthenticationException.class, IdentityController.IdentityServiceAuthenticationException.class})
    ResponseEntity<Map<String, String>> authenticationFailure() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("code", "INVALID_CREDENTIALS", "message", "Email or password is incorrect, or the account is not eligible."));
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
