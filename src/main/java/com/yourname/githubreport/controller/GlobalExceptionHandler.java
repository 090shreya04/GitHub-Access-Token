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

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(OrganizationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleOrgNotFound(OrganizationNotFoundException e) {
        log.warn("Organization not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBody(404, e.getMessage(), "Organization not found on GitHub"));
    }

    @ExceptionHandler(org.springframework.web.server.ServerWebInputException.class)
    public ResponseEntity<Map<String, Object>> handleInputError(org.springframework.web.server.ServerWebInputException e) {
        log.warn("Invalid input: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody(400, "Missing or invalid request parameter", "Ensure the 'org' parameter is provided"));
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimit(RateLimitException e) {
        log.warn("Rate limit exceeded: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(errorBody(429, e.getMessage(), "Wait and retry, or use a token with higher rate limits"));
    }

    @ExceptionHandler(GitHubApiException.class)
    public ResponseEntity<Map<String, Object>> handleGitHubApiError(GitHubApiException e) {
        log.error("GitHub API error ({}): {}", e.getStatusCode(), e.getMessage());
        HttpStatus status = e.getStatusCode() == 401 ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status)
                .body(errorBody(e.getStatusCode(), e.getMessage(), "Check your GITHUB_TOKEN and its permissions"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody(500, e.getMessage(), "An unexpected internal error occurred"));
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
