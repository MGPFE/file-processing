package org.mg.fileprocessing.error;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mg.fileprocessing.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Clock;
import java.time.Instant;

import static org.springframework.http.HttpStatus.*;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final Clock clock;

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return createResponse(NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(UnsupportedContentTypeException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedContentType(UnsupportedContentTypeException ex) {
        return createResponse(BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler({FileHandlingException.class, HttpClientException.class})
    public ResponseEntity<ErrorResponse> handleInternalBusinessErrors(Exception ex) {
        return createResponse(INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(MethodArgumentNotValidException ex) {
        StringBuilder message = new StringBuilder();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            message.append("%s - %s,".formatted(error.getField(), error.getDefaultMessage()));
        }

        return createResponse(BAD_REQUEST, message.substring(0, message.length() - 1));
    }

    @ExceptionHandler(IdempotencyViolationException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyViolationError(IdempotencyViolationException ex) {
        return createResponse(BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsError(UserAlreadyExistsException ex) {
        return createResponse(CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceededError(RateLimitExceededException ex) {
        ResponseEntity<ErrorResponse> response = createResponse(TOO_MANY_REQUESTS, "Request was throttled, retry after %d seconds".formatted(ex.getRetryAfter().getSeconds()));
        response.getHeaders().add("Retry-After", String.valueOf(ex.getRetryAfter().getSeconds()));
        return response;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("Encountered safety net exception: ", ex);

        return createResponse(INTERNAL_SERVER_ERROR, "Server encountered an unexpected exception");
    }

    private ResponseEntity<ErrorResponse> createResponse(HttpStatus status, String message) {
        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                message,
                Instant.now(clock)
        );

        return new ResponseEntity<>(errorResponse, status);
    }
}
