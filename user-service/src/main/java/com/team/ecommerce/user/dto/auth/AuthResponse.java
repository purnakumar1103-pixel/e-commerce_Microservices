package com.team.ecommerce.user.dto.auth;

import com.team.ecommerce.user.dto.user.UserResponse;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UserResponse user
) {
}