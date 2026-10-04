package com.team.ecommerce.product_inventory_service.repository;

import com.team.ecommerce.product_inventory_service.domain.Product;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, UUID id);

    @Query("select p from Product p where p.active = true "
            + "and (:query is null or lower(p.name) like lower(concat('%', :query, '%')) "
            + "or lower(p.sku) like lower(concat('%', :query, '%'))) "
            + "and (:categoryId is null or p.categoryId = :categoryId)")
    Page<Product> searchActive(@Param("query") String query, @Param("categoryId") UUID categoryId, Pageable pageable);

    @Query("select p from Product p where "
            + "(:query is null or lower(p.name) like lower(concat('%', :query, '%')) "
            + "or lower(p.sku) like lower(concat('%', :query, '%'))) "
            + "and (:categoryId is null or p.categoryId = :categoryId)")
    Page<Product> searchAll(@Param("query") String query, @Param("categoryId") UUID categoryId, Pageable pageable);
}
