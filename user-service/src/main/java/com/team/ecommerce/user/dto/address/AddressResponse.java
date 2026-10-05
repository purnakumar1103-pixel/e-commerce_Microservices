package com.team.ecommerce.user.dto.address;

import java.util.UUID;

public record AddressResponse(

        UUID id,
        String label,
        String recipientName,
        String phone,
        String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        String countryCode,
        boolean isDefault

) {
}