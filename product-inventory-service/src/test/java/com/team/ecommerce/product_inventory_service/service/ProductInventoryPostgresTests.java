package com.team.ecommerce.product_inventory_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.team.ecommerce.product_inventory_service.api.ProductDtos.CreateProductRequest;
import com.team.ecommerce.product_inventory_service.api.ProductDtos.ReservationItemRequest;
import com.team.ecommerce.product_inventory_service.api.ProductDtos.ReservationRequest;
import com.team.ecommerce.product_inventory_service.api.ProductDtos.ReservationResponse;
import com.team.ecommerce.product_inventory_service.domain.Inventory;
import com.team.ecommerce.product_inventory_service.repository.CategoryRepository;
import com.team.ecommerce.product_inventory_service.repository.InventoryRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Testcontainers(disabledWithoutDocker = true)
class ProductInventoryPostgresTests {

    private static final UUID BOOKS_CATEGORY = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("product_db")
            .withUsername("product_app")
            .withPassword("test-password");

    @Autowired
    private ProductInventoryService service;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void flywayCreatesSchemaAndSeedsCategories() {
        assertTrue(categoryRepository.count() >= 4);
        assertNotNull(categoryRepository.findById(BOOKS_CATEGORY).orElse(null));
    }

    @Test
    void reservationUpdatesAvailableAndReservedQuantities() {
        var product = service.createProduct(new CreateProductRequest(BOOKS_CATEGORY, unique("PG-SKU"), "Postgres Book",
                null, new BigDecimal("8.50"), "USD", 1));

        ReservationResponse response = service.reserve(new ReservationRequest(UUID.randomUUID(),
                List.of(new ReservationItemRequest(product.id(), 1)))).response();

        Inventory inventory = inventoryRepository.findByProductId(product.id()).orElseThrow();
        assertEquals("RESERVED", response.status());
        assertEquals(0, inventory.getAvailableQuantity());
        assertEquals(1, inventory.getReservedQuantity());
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void concurrentFinalUnitReservationsDoNotOversell() throws Exception {
        var product = service.createProduct(new CreateProductRequest(BOOKS_CATEGORY, unique("PG-SKU"), "Concurrent Book",
                null, new BigDecimal("8.50"), "USD", 1));
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> attemptReservation(start, product.id()));
            Future<Boolean> second = executor.submit(() -> attemptReservation(start, product.id()));
            start.countDown();
            assertEquals(1, List.of(first.get(), second.get()).stream().filter(Boolean::booleanValue).count());
        } finally {
            executor.shutdownNow();
        }

        Inventory inventory = inventoryRepository.findByProductId(product.id()).orElseThrow();
        assertEquals(0, inventory.getAvailableQuantity());
        assertEquals(1, inventory.getReservedQuantity());
    }

    private boolean attemptReservation(CountDownLatch start, UUID productId) throws InterruptedException {
        start.await();
        try {
            service.reserve(new ReservationRequest(UUID.randomUUID(),
                    List.of(new ReservationItemRequest(productId, 1))));
            return true;
        } catch (BusinessException exception) {
            return false;
        }
    }

    private String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
