package com.team.ecommerce.product_inventory_service.repository;

import com.team.ecommerce.product_inventory_service.domain.ReservationItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationItemRepository extends JpaRepository<ReservationItem, UUID> {

    List<ReservationItem> findByReservationIdOrderByProductId(UUID reservationId);
}
