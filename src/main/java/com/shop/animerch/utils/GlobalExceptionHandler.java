
package com.shop.animerch.utils;

import com.shop.animerch.exception.BadRequestException;
import com.shop.animerch.exception.ErrorResponse;
import com.shop.animerch.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("{} {} - {}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage());

        return buildResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            BadRequestException ex,
            HttpServletRequest request
    ) {
        log.warn("{} {} - {}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage());

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": "
                        + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("Validation failed: method={}, path={}, details={}",
                request.getMethod(),
                request.getRequestURI(),
                message);

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request
        );
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidInput(
            Exception ex,
            HttpServletRequest request
    ) {
        log.warn("Invalid request: method={}, path={}, reason={}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage());

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid request data",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unexpected error: method={}, path={}",
                request.getMethod(),
                request.getRequestURI(),
                ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                request
        );
    }
}
