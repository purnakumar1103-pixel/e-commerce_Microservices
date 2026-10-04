package com.team.ecommerce.product_inventory_service.api;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        Instant timestamp,
        int status,
        String errorCode,
        String message,
        String path,
        String correlationId,
        Map<String, String> fieldErrors) {
}
