package com.team.ecommerce.user.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(

        @NotBlank
        @Size(min = 2, max = 120)
        String fullName,

        @Size(max = 30)
        String phone

) {
}