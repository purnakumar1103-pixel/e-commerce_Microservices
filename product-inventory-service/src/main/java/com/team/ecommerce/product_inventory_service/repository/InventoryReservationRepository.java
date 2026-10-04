package com.team.ecommerce.product_inventory_service.repository;

import com.team.ecommerce.product_inventory_service.domain.InventoryReservation;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {

    Optional<InventoryReservation> findByOrderReference(UUID orderReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from InventoryReservation r where r.id = :id")
    Optional<InventoryReservation> findByIdForUpdate(@Param("id") UUID id);
}
