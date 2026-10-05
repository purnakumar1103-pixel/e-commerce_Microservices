package com.team.ecommerce.user.dto.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(

        @NotBlank
        @Size(max = 40)
        String label,

        @NotBlank
        @Size(max = 120)
        String recipientName,

        @NotBlank
        @Size(max = 30)
        String phone,

        @NotBlank
        @Size(max = 160)
        String line1,

        @Size(max = 160)
        String line2,

        @NotBlank
        @Size(max = 80)
        String city,

        @NotBlank
        @Size(max = 80)
        String state,

        @NotBlank
        @Size(max = 20)
        String postalCode,

        @NotBlank
        @Size(min = 2, max = 2)
        String countryCode,

        boolean isDefault

) {
}