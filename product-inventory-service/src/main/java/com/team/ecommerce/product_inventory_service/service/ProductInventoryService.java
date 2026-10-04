package com.team.ecommerce.product_inventory_service.service;

import com.team.ecommerce.product_inventory_service.api.ProductDtos.CreateProductRequest;
import com.team.ecommerce.product_inventory_service.api.ProductDtos.ProductResponse;
import com.team.ecommerce.product_inventory_service.api.ProductDtos.ReservationItemRequest;
import com.team.ecommerce.product_inventory_service.api.ProductDtos.ReservationRequest;
import com.team.ecommerce.product_inventory_service.api.ProductDtos.ReservationResponse;
import com.team.ecommerce.product_inventory_service.api.ProductDtos.ReservedItemResponse;
import com.team.ecommerce.product_inventory_service.api.ProductDtos.StockAdjustmentRequest;
import com.team.ecommerce.product_inventory_service.api.ProductDtos.UpdateProductRequest;
import com.team.ecommerce.product_inventory_service.domain.Inventory;
import com.team.ecommerce.product_inventory_service.domain.InventoryReservation;
import com.team.ecommerce.product_inventory_service.domain.Category;
import com.team.ecommerce.product_inventory_service.domain.Product;
import com.team.ecommerce.product_inventory_service.domain.ReservationItem;
import com.team.ecommerce.product_inventory_service.domain.ReservationStatus;
import com.team.ecommerce.product_inventory_service.domain.StockAdjustment;
import com.team.ecommerce.product_inventory_service.repository.InventoryRepository;
import com.team.ecommerce.product_inventory_service.repository.InventoryReservationRepository;
import com.team.ecommerce.product_inventory_service.repository.CategoryRepository;
import com.team.ecommerce.product_inventory_service.repository.ProductRepository;
import com.team.ecommerce.product_inventory_service.repository.ReservationItemRepository;
import com.team.ecommerce.product_inventory_service.repository.StockAdjustmentRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ProductInventoryService {

    public record ReservationOutcome(ReservationResponse response, boolean created) {
    }

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final ReservationItemRepository reservationItemRepository;
    private final StockAdjustmentRepository stockAdjustmentRepository;
    private final TransactionTemplate transactionTemplate;

    public ProductInventoryService(ProductRepository productRepository,
                                   CategoryRepository categoryRepository,
                                   InventoryRepository inventoryRepository,
                                   InventoryReservationRepository reservationRepository,
                                   ReservationItemRepository reservationItemRepository,
                                   StockAdjustmentRepository stockAdjustmentRepository,
                                   PlatformTransactionManager transactionManager) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.reservationItemRepository = reservationItemRepository;
        this.stockAdjustmentRepository = stockAdjustmentRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(String query, UUID categoryId, Pageable pageable, boolean includeInactive) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        Page<Product> products = includeInactive
                ? productRepository.searchAll(normalizedQuery, categoryId, pageable)
                : productRepository.searchActive(normalizedQuery, categoryId, pageable);
        return products.map(this::toProductResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID id, boolean includeInactive) {
        Product product = productRepository.findById(id)
                .filter(value -> includeInactive || value.isActive())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND",
                        "Product was not found"));
        return toProductResponse(product);
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        String sku = normalizeSku(request.sku());
        ensureSkuAvailable(sku, null);
        Category category = getActiveCategory(request.categoryId());
        Product product = new Product(request.categoryId(), sku, request.name().trim(), request.description(),
                request.price(), request.currency());
        productRepository.saveAndFlush(product);
        inventoryRepository.save(new Inventory(product.getId(), request.startingQuantity()));
        return toProductResponse(product, category);
    }

    @Transactional
    public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND",
                        "Product was not found"));
        Category category = getActiveCategory(request.categoryId());
        product.update(request.categoryId(), request.name().trim(), request.description(),
                request.price(), request.currency(), request.active());
        return toProductResponse(product, category);
    }

    @Transactional
    public ProductResponse deactivateProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND",
                        "Product was not found"));
        product.deactivate();
        return toProductResponse(product);
    }

    @Transactional
    public ProductResponse adjustStock(UUID productId, StockAdjustmentRequest request, String changedBy) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND",
                        "Product was not found"));
        Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "INVENTORY_NOT_FOUND",
                        "Inventory was not found"));
        if (request.quantityDelta() == 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                    "quantityDelta must not be zero");
        }
        try {
            inventory.adjust(request.quantityDelta());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "INVALID_STOCK_ADJUSTMENT", exception.getMessage());
        }
        stockAdjustmentRepository.save(new StockAdjustment(productId, request.quantityDelta(),
                request.reason().trim(), changedBy));
        return toProductResponse(product);
    }

    public ReservationOutcome reserve(ReservationRequest request) {
        try {
            return Objects.requireNonNull(transactionTemplate.execute(status -> reserveInTransaction(request)));
        } catch (DataIntegrityViolationException exception) {
            return reservationRepository.findByOrderReference(request.orderReference())
                    .map(reservation -> new ReservationOutcome(toReservationResponse(reservation), false))
                    .orElseThrow(() -> exception);
        }
    }

    private ReservationOutcome reserveInTransaction(ReservationRequest request) {
        var existing = reservationRepository.findByOrderReference(request.orderReference());
        if (existing.isPresent()) {
            return new ReservationOutcome(toReservationResponse(existing.get()), false);
        }

        Map<UUID, Integer> quantities = aggregateQuantities(request.items());
        Map<UUID, Product> products = new LinkedHashMap<>();
        Map<UUID, Inventory> inventories = new LinkedHashMap<>();
        for (Map.Entry<UUID, Integer> entry : quantities.entrySet()) {
            Product product = productRepository.findById(entry.getKey())
                    .orElseThrow(() -> new BusinessException(HttpStatus.CONFLICT, "PRODUCT_NOT_FOUND",
                            "One or more products do not exist"));
            if (!product.isActive()) {
                throw new BusinessException(HttpStatus.CONFLICT, "PRODUCT_INACTIVE",
                        "Reservation includes an inactive product");
            }
            Inventory inventory = inventoryRepository.findByProductIdForUpdate(entry.getKey())
                    .orElseThrow(() -> new BusinessException(HttpStatus.CONFLICT, "INVENTORY_NOT_FOUND",
                            "Inventory is not configured for one or more products"));
            if (inventory.getAvailableQuantity() < entry.getValue()) {
                throw new BusinessException(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK",
                        "Requested quantity is unavailable");
            }
            if (!products.isEmpty() && !products.values().iterator().next().getCurrency().equals(product.getCurrency())) {
                throw new BusinessException(HttpStatus.CONFLICT, "CURRENCY_MISMATCH",
                        "All products in one reservation must use the same currency");
            }
            products.put(entry.getKey(), product);
            inventories.put(entry.getKey(), inventory);
        }

        InventoryReservation reservation = reservationRepository.saveAndFlush(
                new InventoryReservation(request.orderReference()));
        for (Map.Entry<UUID, Integer> entry : quantities.entrySet()) {
            UUID productId = entry.getKey();
            int quantity = entry.getValue();
            inventories.get(productId).reserve(quantity);
            reservationItemRepository.save(new ReservationItem(reservation.getId(), productId, quantity,
                    products.get(productId).getPrice()));
        }
        return new ReservationOutcome(toReservationResponse(reservation), true);
    }

    @Transactional
    public ReservationResponse release(UUID reservationId) {
        InventoryReservation reservation = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "RESERVATION_NOT_FOUND",
                        "Reservation was not found"));
        List<ReservationItem> items = reservationItemRepository.findByReservationIdOrderByProductId(reservationId);
        if (reservation.getStatus() == ReservationStatus.RESERVED) {
            for (ReservationItem item : items) {
                Inventory inventory = inventoryRepository.findByProductIdForUpdate(item.getProductId())
                        .orElseThrow(() -> new BusinessException(HttpStatus.CONFLICT, "INVENTORY_NOT_FOUND",
                                "Inventory is not configured for a reserved product"));
                inventory.release(item.getQuantity());
            }
            reservation.release();
        }
        return toReservationResponse(reservation);
    }

    private Map<UUID, Integer> aggregateQuantities(List<ReservationItemRequest> items) {
        Map<UUID, Integer> quantities = new HashMap<>();
        for (ReservationItemRequest item : items) {
            if (quantities.containsKey(item.productId())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                        "Duplicate product IDs are not allowed in one reservation");
            }
            int current = quantities.getOrDefault(item.productId(), 0);
            long next = (long) current + item.quantity();
            if (next > Integer.MAX_VALUE) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                        "Requested quantity is too large");
            }
            quantities.put(item.productId(), (int) next);
        }
        return quantities.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (left, right) -> left, LinkedHashMap::new));
    }

    private ProductResponse toProductResponse(Product product) {
        return toProductResponse(product, categoryRepository.findById(product.getCategoryId()).orElse(null));
    }

    private ProductResponse toProductResponse(Product product, Category category) {
        int quantity = inventoryRepository.findByProductId(product.getId())
                .map(Inventory::getAvailableQuantity).orElse(0);
        return new ProductResponse(product.getId(), product.getCategoryId(), category == null ? null : category.getName(),
                product.getSku(), product.getName(),
                product.getDescription(), product.getPrice(), product.getCurrency(), product.isActive(), quantity);
    }

    private ReservationResponse toReservationResponse(InventoryReservation reservation) {
        List<ReservationItem> items = reservationItemRepository.findByReservationIdOrderByProductId(reservation.getId());
        Map<UUID, Product> products = productRepository.findAllById(items.stream()
                        .map(ReservationItem::getProductId).toList()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        List<ReservedItemResponse> responses = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        String currency = null;
        for (ReservationItem item : items) {
            Product product = products.get(item.getProductId());
            String itemCurrency = product == null ? currency : product.getCurrency();
            BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(lineTotal);
            if (currency == null) {
                currency = itemCurrency;
            }
            responses.add(new ReservedItemResponse(item.getProductId(), product == null ? null : product.getSku(),
                    product == null ? null : product.getName(), item.getQuantity(), item.getUnitPrice(),
                    itemCurrency, lineTotal));
        }
        return new ReservationResponse(reservation.getId(), reservation.getOrderReference(),
                reservation.getStatus().name(), responses, total, currency);
    }

    private Category getActiveCategory(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .filter(Category::isActive)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                        "Category does not exist or is inactive"));
    }

    private void ensureSkuAvailable(String sku, UUID currentId) {
        boolean exists = currentId == null
                ? productRepository.existsBySkuIgnoreCase(sku)
                : productRepository.existsBySkuIgnoreCaseAndIdNot(sku, currentId);
        if (exists) {
            throw new BusinessException(HttpStatus.CONFLICT, "SKU_ALREADY_EXISTS", "SKU is already in use");
        }
    }

    private String normalizeSku(String sku) {
        return sku.trim().toUpperCase();
    }
}
