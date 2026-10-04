package com.team.ecommerce.product_inventory_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Inventory() {
    }

    public Inventory(UUID productId, int availableQuantity) {
        this.productId = productId;
        this.availableQuantity = availableQuantity;
    }

    public UUID getProductId() { return productId; }
    public int getAvailableQuantity() { return availableQuantity; }
    public int getReservedQuantity() { return reservedQuantity; }
    public long getVersion() { return version; }

    public void adjust(int quantityChange) {
        long next = (long) availableQuantity + quantityChange;
        if (next < 0 || next > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Available stock cannot be negative or exceed the integer limit");
        }
        availableQuantity = (int) next;
    }

    public void reserve(int quantity) {
        if (quantity <= 0 || availableQuantity < quantity) {
            throw new IllegalArgumentException("Insufficient stock");
        }
        availableQuantity -= quantity;
        reservedQuantity = Math.addExact(reservedQuantity, quantity);
    }

    public void release(int quantity) {
        if (quantity <= 0 || reservedQuantity < quantity) {
            throw new IllegalArgumentException("Reserved stock cannot become negative");
        }
        reservedQuantity -= quantity;
        adjust(quantity);
    }

    @jakarta.persistence.PrePersist
    @jakarta.persistence.PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
