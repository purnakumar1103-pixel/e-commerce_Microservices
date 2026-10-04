package com.team.ecommerce.product_inventory_service.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class ProductDtos {

    private ProductDtos() {
    }

    public record CreateProductRequest(
            @NotNull UUID categoryId,
            @NotBlank @Size(max = 64) String sku,
            @NotBlank @Size(max = 160) String name,
            String description,
            @NotNull @DecimalMin(value = "0.00") @Digits(integer = 17, fraction = 2) BigDecimal price,
            @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
            @NotNull @PositiveOrZero Integer startingQuantity) {
    }

    public record UpdateProductRequest(
            @NotNull UUID categoryId,
            @NotBlank @Size(max = 160) String name,
            String description,
            @NotNull @DecimalMin(value = "0.00") @Digits(integer = 17, fraction = 2) BigDecimal price,
            @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
            @NotNull Boolean active) {
    }

    public record StockAdjustmentRequest(
            @NotNull Integer quantityDelta,
            @NotBlank @Size(max = 500) String reason) {
    }

    public record ProductResponse(
            UUID id,
            UUID categoryId,
            String categoryName,
            String sku,
            String name,
            String description,
            BigDecimal price,
            String currency,
            boolean active,
            int availableQuantity) {
    }

    public record ProductPageResponse(
            List<ProductResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages) {
    }

    public record ReservationItemRequest(
            @NotNull UUID productId,
            @NotNull @Positive Integer quantity) {
    }

    public record ReservationRequest(
            @NotNull UUID orderReference,
            @NotEmpty @Size(max = 100) List<@Valid ReservationItemRequest> items) {
    }

    public record ReservedItemResponse(
            UUID productId,
            String sku,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            String currency,
            BigDecimal lineTotal) {
    }

    public record ReservationResponse(
            UUID reservationId,
            UUID orderReference,
            String status,
            List<ReservedItemResponse> items,
            BigDecimal total,
            String currency) {
    }
}
