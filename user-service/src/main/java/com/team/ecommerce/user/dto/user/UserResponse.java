package com.team.ecommerce.user.dto.user;

import com.team.ecommerce.user.entity.Role;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        Role role,
        boolean active,
        OffsetDateTime createdAt
) {
}