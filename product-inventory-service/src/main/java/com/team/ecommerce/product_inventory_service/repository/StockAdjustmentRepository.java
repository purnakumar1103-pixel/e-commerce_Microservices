package com.team.ecommerce.product_inventory_service.repository;

import com.team.ecommerce.product_inventory_service.domain.StockAdjustment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, UUID> {
}
