package com.team.ecommerce.product_inventory_service.api;

import com.team.ecommerce.product_inventory_service.api.ProductDtos.CreateProductRequest;
import com.team.ecommerce.product_inventory_service.api.ProductDtos.ProductResponse;
import com.team.ecommerce.product_inventory_service.api.ProductDtos.ProductPageResponse;
import com.team.ecommerce.product_inventory_service.api.ProductDtos.ReservationRequest;
import com.team.ecommerce.product_inventory_service.api.ProductDtos.ReservationResponse;
import com.team.ecommerce.product_inventory_service.api.ProductDtos.StockAdjustmentRequest;
import com.team.ecommerce.product_inventory_service.api.ProductDtos.UpdateProductRequest;
import com.team.ecommerce.product_inventory_service.service.ProductInventoryService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductInventoryController {

    private final ProductInventoryService service;
    private final int maxPageSize;

    public ProductInventoryController(ProductInventoryService service,
                                      @Value("${app.pagination.max-page-size:50}") int maxPageSize) {
        this.service = service;
        this.maxPageSize = maxPageSize;
    }

    @GetMapping("/api/v1/products")
    public ProductPageResponse products(@RequestParam(name = "q", required = false) String query,
                                          @RequestParam(required = false) UUID categoryId,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          @RequestParam(defaultValue = "name") String sort) {
        return pageResponse(service.searchProducts(query, categoryId, pageable(page, size, sort), false));
    }

    @GetMapping("/api/v1/products/{id}")
    public ProductResponse product(@PathVariable UUID id) {
        return service.getProduct(id, false);
    }

    @GetMapping("/api/v1/admin/products")
    @SecurityRequirement(name = "bearerAuth")
    public ProductPageResponse adminProducts(@RequestParam(name = "q", required = false) String query,
                                               @RequestParam(required = false) UUID categoryId,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size,
                                               @RequestParam(defaultValue = "name") String sort) {
        return pageResponse(service.searchProducts(query, categoryId, pageable(page, size, sort), true));
    }

    @PostMapping("/api/v1/admin/products")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createProduct(request));
    }

    @PutMapping("/api/v1/admin/products/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ProductResponse updateProduct(@PathVariable UUID id, @Valid @RequestBody UpdateProductRequest request) {
        return service.updateProduct(id, request);
    }

    @PatchMapping("/api/v1/admin/products/{id}/stock")
    @SecurityRequirement(name = "bearerAuth")
    public ProductResponse adjustStock(@PathVariable UUID id, @Valid @RequestBody StockAdjustmentRequest request,
                                       Authentication authentication) {
        String changedBy = authentication == null ? "unknown-admin" : authentication.getName();
        return service.adjustStock(id, request, changedBy);
    }

    @DeleteMapping("/api/v1/admin/products/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        service.deactivateProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/internal/v1/inventory/reservations")
    @SecurityRequirement(name = "internalApiKey")
    public ResponseEntity<ReservationResponse> reserve(@Valid @RequestBody ReservationRequest request) {
        ProductInventoryService.ReservationOutcome outcome = service.reserve(request);
        return ResponseEntity.status(outcome.created() ? HttpStatus.CREATED : HttpStatus.OK).body(outcome.response());
    }

    @PostMapping("/internal/v1/inventory/reservations/{id}/release")
    @SecurityRequirement(name = "internalApiKey")
    public ReservationResponse release(@PathVariable UUID id) {
        return service.release(id);
    }

    private Pageable pageable(int page, int size, String sort) {
        if (page < 0 || size < 1 || size > maxPageSize) {
            throw new com.team.ecommerce.product_inventory_service.service.BusinessException(
                    HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Page must be non-negative and size must be within limits");
        }
        String[] parts = sort.split(",", -1);
        if (parts.length > 2 || !java.util.Set.of("name", "price", "createdAt").contains(parts[0])) {
            throw new com.team.ecommerce.product_inventory_service.service.BusinessException(
                    HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Sort must be name, price, or createdAt");
        }
        Sort.Direction direction = parts.length == 2 ? Sort.Direction.fromOptionalString(parts[1]).orElse(null)
                : Sort.Direction.ASC;
        if (direction == null) {
            throw new com.team.ecommerce.product_inventory_service.service.BusinessException(
                    HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Sort direction must be asc or desc");
        }
        return PageRequest.of(page, size, Sort.by(direction, parts[0]));
    }

    private ProductPageResponse pageResponse(Page<ProductResponse> page) {
        return new ProductPageResponse(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
