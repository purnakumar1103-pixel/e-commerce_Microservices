package com.team.ecommerce.user.mapper;

import com.team.ecommerce.user.dto.address.AddressResponse;
import com.team.ecommerce.user.entity.Address;

public class AddressMapper {

    public static AddressResponse toResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getLabel(),
                address.getRecipientName(),
                address.getPhone(),
                address.getLine1(),
                address.getLine2(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountryCode(),
                address.isDefault()
        );
    }
}