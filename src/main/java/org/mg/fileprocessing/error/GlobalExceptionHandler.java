package org.mg.fileprocessing.error;

import org.mg.fileprocessing.exception.FileHandlingException;
import org.mg.fileprocessing.exception.HttpClientException;
import org.mg.fileprocessing.exception.ResourceNotFoundException;
import org.mg.fileprocessing.exception.UnsupportedContentTypeException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;

import static org.springframework.http.HttpStatus.*;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                NOT_FOUND.value(),
                ex.getMessage(),
                Instant.now()
        );

        return new ResponseEntity<>(errorResponse, NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleUnsupportedContentType(UnsupportedContentTypeException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                BAD_REQUEST.value(),
                ex.getMessage(),
                Instant.now()
        );

        return new ResponseEntity<>(errorResponse, BAD_REQUEST);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleFileHandlingException(FileHandlingException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                INTERNAL_SERVER_ERROR.value(),
                ex.getMessage(),
                Instant.now()
        );

        return new ResponseEntity<>(errorResponse, INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleHttpClientException(HttpClientException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                INTERNAL_SERVER_ERROR.value(),
                ex.getMessage(),
                Instant.now()
        );

        return new ResponseEntity<>(errorResponse, INTERNAL_SERVER_ERROR);
    }
}
