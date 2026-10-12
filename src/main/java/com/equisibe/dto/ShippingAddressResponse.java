package com.equisibe.dto;

public record ShippingAddressResponse(
        Long id,
        String recipientName,
        String phone,
        String addressLine1,
        String addressLine2,
        String city,
        String province,
        String postalCode,
        String countryCode
) {
}