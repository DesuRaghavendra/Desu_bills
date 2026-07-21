package com.system.exception;

import com.system.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        log.warn("Validation failure: {}", message);
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("VALIDATION_FAILURE")
                .message(message)
                .timestamp(Instant.now().toString())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        log.warn("Registration conflict: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("USER_ALREADY_EXISTS")
                .message(ex.getMessage())
                .timestamp(Instant.now().toString())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(ForbiddenException ex) {
        log.warn("Access forbidden: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("FORBIDDEN")
                .message(ex.getMessage())
                .timestamp(Instant.now().toString())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex) {
        log.warn("Login failure: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("BAD_CREDENTIALS")
                .message("Invalid email or password")
                .timestamp(Instant.now().toString())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication error: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("UNAUTHORIZED")
                .message(ex.getMessage())
                .timestamp(Instant.now().toString())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("VALIDATION_FAILURE")
                .message(ex.getMessage())
                .timestamp(Instant.now().toString())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(OcrProcessingException.class)
    public ResponseEntity<ErrorResponse> handleOcrProcessingException(OcrProcessingException ex) {
        log.error("OCR process failure: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("OCR_PROCESSING_FAILURE")
                .message(ex.getMessage())
                .timestamp(Instant.now().toString())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(InvalidImageException.class)
    public ResponseEntity<ErrorResponse> handleInvalidImageException(InvalidImageException ex) {
        log.warn("Invalid image failure: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("INVALID_IMAGE")
                .message(ex.getMessage())
                .timestamp(Instant.now().toString())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HeaderMismatchException.class)
    public ResponseEntity<ErrorResponse> handleHeaderMismatchException(HeaderMismatchException ex) {
        log.warn("Header mismatch failure: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("HEADER_MISMATCH")
                .message(ex.getMessage())
                .timestamp(Instant.now().toString())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(DuplicateTableNameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateTableNameException(DuplicateTableNameException ex) {
        log.warn("Duplicate table name failure: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("DUPLICATE_TABLE_NAME")
                .message(ex.getMessage())
                .timestamp(Instant.now().toString())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailableException(ServiceUnavailableException ex) {
        log.error("Service unavailable failure: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("SERVICE_UNAVAILABLE")
                .message(ex.getMessage())
                .timestamp(Instant.now().toString())
                .build();
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    @ExceptionHandler(org.springframework.web.client.ResourceAccessException.class)
    public ResponseEntity<ErrorResponse> handleResourceAccessException(org.springframework.web.client.ResourceAccessException ex) {
        log.error("Downstream network timeout/connection failure: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("SERVICE_UNAVAILABLE")
                .message("Downstream service is currently unreachable or timed out.")
                .timestamp(Instant.now().toString())
                .build();
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled exception occurred", ex);
        
        ErrorResponse error = ErrorResponse.builder()
                .errorCode("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred: " + ex.getMessage())
                .timestamp(Instant.now().toString())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
