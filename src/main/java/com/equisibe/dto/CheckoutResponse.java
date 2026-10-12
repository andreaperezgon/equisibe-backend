package com.equisibe.dto;

public record CheckoutResponse(
        ShippingAddressResponse shippingAddress,
        CartValidationResponse cart
) {
}