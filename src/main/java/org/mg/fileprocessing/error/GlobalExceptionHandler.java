package org.mg.fileprocessing.error;

import lombok.extern.slf4j.Slf4j;
import org.mg.fileprocessing.exception.FileHandlingException;
import org.mg.fileprocessing.exception.HttpClientException;
import org.mg.fileprocessing.exception.ResourceNotFoundException;
import org.mg.fileprocessing.exception.UnsupportedContentTypeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;

import static org.springframework.http.HttpStatus.*;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("Encountered safety net exception: ", ex);

        return createResponse(INTERNAL_SERVER_ERROR, "Server encountered an unexpected exception");
    }

    private ResponseEntity<ErrorResponse> createResponse(HttpStatus status, String message) {
        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                message,
                Instant.now()
        );

        return new ResponseEntity<>(errorResponse, status);
    }
}
