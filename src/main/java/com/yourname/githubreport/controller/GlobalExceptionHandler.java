package com.yourname.githubreport.controller;

import com.yourname.githubreport.exception.GitHubApiException;
import com.yourname.githubreport.exception.OrganizationNotFoundException;
import com.yourname.githubreport.exception.RateLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Handles all exceptions thrown by controllers and converts them
 * into clean, structured JSON error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(OrganizationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleOrgNotFound(OrganizationNotFoundException e) {
        log.warn("Organization not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody(404, e.getMessage(), "Check the organization name and try again"));
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(ServerWebInputException e) {
        log.warn("Missing or invalid request parameter: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody(400, "Missing or invalid request parameter", "Ensure the 'org' query parameter is provided"));
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimit(RateLimitException e) {
        log.warn("GitHub rate limit exceeded: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(errorBody(429, e.getMessage(), "Wait a moment and retry, or use a token with a higher rate limit"));
    }

    @ExceptionHandler(GitHubApiException.class)
    public ResponseEntity<Map<String, Object>> handleGitHubApiError(GitHubApiException e) {
        log.error("GitHub API error (status {}): {}", e.getStatusCode(), e.getMessage());
        HttpStatus status = e.getStatusCode() == 401 ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status)
                .body(errorBody(e.getStatusCode(), e.getMessage(), "Check your GITHUB_TOKEN and its scopes"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedError(Exception e) {
        log.error("Unexpected error occurred: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody(500, "An unexpected error occurred", "Please try again later"));
    }

    private Map<String, Object> errorBody(int status, String message, String hint) {
        return Map.of(
                "status", status,
                "error", message != null ? message : "Unknown error",
                "hint", hint,
                "timestamp", LocalDateTime.now().toString()
        );
    }
}
