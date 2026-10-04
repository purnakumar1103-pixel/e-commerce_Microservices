package com.team.ecommerce.product_inventory_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.team.ecommerce.product_inventory_service.api.ProductDtos.CreateProductRequest;
import com.team.ecommerce.product_inventory_service.api.ProductDtos.ReservationItemRequest;
import com.team.ecommerce.product_inventory_service.api.ProductDtos.ReservationRequest;
import com.team.ecommerce.product_inventory_service.api.ProductDtos.ReservationResponse;
import com.team.ecommerce.product_inventory_service.domain.Category;
import com.team.ecommerce.product_inventory_service.domain.Inventory;
import com.team.ecommerce.product_inventory_service.repository.CategoryRepository;
import com.team.ecommerce.product_inventory_service.repository.InventoryRepository;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ProductInventoryServiceIntegrationTests {

    private static final UUID BOOKS_CATEGORY = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private ProductInventoryService service;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void seedCategory() {
        categoryRepository.findById(BOOKS_CATEGORY)
                .orElseGet(() -> categoryRepository.save(new Category(BOOKS_CATEGORY, "Books")));
    }

    @Test
    void reserveAndReleaseAreIdempotent() {
        var product = service.createProduct(new CreateProductRequest(BOOKS_CATEGORY, unique("SKU"), "Keyboard", null,
                new BigDecimal("25.00"), "USD", 5));
        var request = new ReservationRequest(UUID.randomUUID(),
                List.of(new ReservationItemRequest(product.id(), 2)));

        ReservationResponse reserved = service.reserve(request).response();
        assertEquals("RESERVED", reserved.status());
        assertTrue(reserved.total().compareTo(new BigDecimal("50.00")) == 0);
        assertEquals(3, stock(product.id()));
        assertEquals(2, reservedStock(product.id()));

        ReservationResponse released = service.release(reserved.reservationId());
        assertEquals("RELEASED", released.status());
        assertEquals(5, stock(product.id()));
        assertEquals(0, reservedStock(product.id()));

        service.release(reserved.reservationId());
        assertEquals(5, stock(product.id()));
        assertEquals(0, reservedStock(product.id()));
    }

    @Test
    void insufficientStockDoesNotPartiallyReserveItems() {
        var first = service.createProduct(new CreateProductRequest(BOOKS_CATEGORY, unique("SKU"), "Mouse", null,
                new BigDecimal("10.00"), "USD", 1));
        var second = service.createProduct(new CreateProductRequest(BOOKS_CATEGORY, unique("SKU"), "Monitor", null,
                new BigDecimal("100.00"), "USD", 4));
        var request = new ReservationRequest(UUID.randomUUID(), List.of(
                new ReservationItemRequest(first.id(), 1), new ReservationItemRequest(second.id(), 5)));

        assertThrows(BusinessException.class, () -> service.reserve(request));
        assertEquals(1, stock(first.id()));
        assertEquals(4, stock(second.id()));
    }

    @Test
    void duplicateOrderReferenceReturnsOriginalReservationWithoutChangingStockAgain() {
        var product = service.createProduct(new CreateProductRequest(BOOKS_CATEGORY, unique("SKU"), "Idempotent Book", null,
                new BigDecimal("10.00"), "USD", 3));
        UUID orderReference = UUID.randomUUID();
        var request = new ReservationRequest(orderReference,
                List.of(new ReservationItemRequest(product.id(), 2)));

        var first = service.reserve(request);
        var second = service.reserve(request);

        assertTrue(first.created());
        assertFalse(second.created());
        assertEquals(first.response().reservationId(), second.response().reservationId());
        assertEquals(1, stock(product.id()));
        assertEquals(2, reservedStock(product.id()));
    }

    @Test
    void duplicateProductIdsAndMixedCurrenciesAreRejected() {
        var first = service.createProduct(new CreateProductRequest(BOOKS_CATEGORY, unique("SKU"), "USD Book", null,
                new BigDecimal("10.00"), "USD", 2));
        var second = service.createProduct(new CreateProductRequest(BOOKS_CATEGORY, unique("SKU"), "EUR Book", null,
                new BigDecimal("10.00"), "EUR", 2));

        assertThrows(BusinessException.class, () -> service.reserve(new ReservationRequest(UUID.randomUUID(),
                List.of(new ReservationItemRequest(first.id(), 1), new ReservationItemRequest(first.id(), 1)))));
        assertThrows(BusinessException.class, () -> service.reserve(new ReservationRequest(UUID.randomUUID(),
                List.of(new ReservationItemRequest(first.id(), 1), new ReservationItemRequest(second.id(), 1)))));
        assertEquals(2, stock(first.id()));
        assertEquals(2, stock(second.id()));
    }

    private int stock(UUID productId) {
        return inventoryRepository.findByProductId(productId).map(Inventory::getAvailableQuantity).orElseThrow();
    }

    private int reservedStock(UUID productId) {
        return inventoryRepository.findByProductId(productId).map(Inventory::getReservedQuantity).orElseThrow();
    }

    private String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
