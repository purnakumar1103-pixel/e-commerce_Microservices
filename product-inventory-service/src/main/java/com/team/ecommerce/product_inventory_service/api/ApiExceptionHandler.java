package com.team.ecommerce.product_inventory_service.api;

import com.team.ecommerce.product_inventory_service.config.CorrelationIdFilter;
import com.team.ecommerce.product_inventory_service.service.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiError> handleBusiness(BusinessException exception, HttpServletRequest request) {
        return response(exception.getStatus(), exception.getCode(), exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(error -> error.getField(), error -> error.getDefaultMessage(),
                        (first, second) -> first, java.util.LinkedHashMap::new));
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", request, fieldErrors);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<ApiError> handleMissingHeader(MissingRequestHeaderException exception, HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "MISSING_SERVICE_CREDENTIAL", "Required service header is missing",
                request, Map.of(exception.getHeaderName(), "must be provided"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleConstraint(DataIntegrityViolationException exception, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "DATA_CONSTRAINT_VIOLATION", "The request conflicts with existing data",
                request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred",
                request, Map.of());
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String code, String message,
                                              HttpServletRequest request, Map<String, String> fieldErrors) {
        String correlationId = (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        if (correlationId == null) {
            correlationId = request.getHeader(CorrelationIdFilter.HEADER);
        }
        ApiError error = new ApiError(Instant.now(), status.value(), code, message, request.getRequestURI(),
                correlationId, fieldErrors);
        return ResponseEntity.status(status).body(error);
    }
}
