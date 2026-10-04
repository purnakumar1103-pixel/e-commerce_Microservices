package com.team.ecommerce.user.repository;

import com.team.ecommerce.user.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findAllByUserId(UUID userId);

    Optional<Address> findByIdAndUserId(UUID id, UUID userId);
}