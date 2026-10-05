package com.team.ecommerce.user.mapper;

import com.team.ecommerce.user.dto.user.UserResponse;
import com.team.ecommerce.user.entity.User;

public class UserMapper {

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}