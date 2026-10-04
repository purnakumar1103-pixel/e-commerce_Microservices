package com.team.ecommerce.product_inventory_service.repository;

import com.team.ecommerce.product_inventory_service.domain.Category;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
}
