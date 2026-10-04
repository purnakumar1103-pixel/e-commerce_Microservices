package com.team.ecommerce.product_inventory_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.ecommerce.product_inventory_service.api.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class ApiErrorWriter {

    private final ObjectMapper objectMapper;

    public ApiErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletRequest request, HttpServletResponse response, int status,
                      String errorCode, String message, Map<String, String> fieldErrors) throws IOException {
        String correlationId = (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        if (correlationId == null) {
            correlationId = request.getHeader(CorrelationIdFilter.HEADER);
        }
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ApiError(Instant.now(), status, errorCode, message,
                request.getRequestURI(), correlationId, fieldErrors));
    }
}
