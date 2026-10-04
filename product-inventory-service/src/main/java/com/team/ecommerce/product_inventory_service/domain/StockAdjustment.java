package com.team.ecommerce.product_inventory_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_adjustments")
public class StockAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "quantity_change", nullable = false)
    private int quantityChange;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "changed_by", nullable = false, length = 120)
    private String changedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StockAdjustment() {
    }

    public StockAdjustment(UUID productId, int quantityChange, String reason, String changedBy) {
        this.productId = productId;
        this.quantityChange = quantityChange;
        this.reason = reason;
        this.changedBy = changedBy;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
