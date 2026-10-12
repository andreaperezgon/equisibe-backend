package com.equisibe.service;

import com.equisibe.dto.CartValidationRequest;
import com.equisibe.dto.CartValidationResponse;
import com.equisibe.dto.CheckoutRequest;
import com.equisibe.dto.CheckoutResponse;
import com.equisibe.dto.ShippingAddressResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class CheckoutService {

    private final ShippingAddressService shippingAddressService;
    private final CartValidationService cartValidationService;

    public CheckoutService(
            ShippingAddressService shippingAddressService,
            CartValidationService cartValidationService) {

        this.shippingAddressService = shippingAddressService;
        this.cartValidationService = cartValidationService;
    }

    public CheckoutResponse validate(
            String email,
            CheckoutRequest request) {

        ShippingAddressResponse address =
                shippingAddressService.getAddress(
                        email,
                        request.shippingAddressId()
                );

        if (!"ES".equals(address.countryCode())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Solo realizamos envíos a España."
            );
        }

        CartValidationRequest cartRequest =
                new CartValidationRequest(request.items());

        CartValidationResponse cart =
                cartValidationService.validate(cartRequest);

        return new CheckoutResponse(address, cart);
    }
}