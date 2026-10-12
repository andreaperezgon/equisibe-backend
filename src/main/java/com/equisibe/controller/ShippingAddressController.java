package com.equisibe.controller;

import com.equisibe.dto.ShippingAddressRequest;
import com.equisibe.dto.ShippingAddressResponse;
import com.equisibe.service.ShippingAddressService;

import jakarta.validation.Valid;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/addresses")
public class ShippingAddressController {

    private final ShippingAddressService addressService;

    public ShippingAddressController(
            ShippingAddressService addressService) {

        this.addressService = addressService;
    }

    @GetMapping
    public List<ShippingAddressResponse> getAddresses(
            Principal principal) {

        return addressService.getAddresses(principal.getName());
    }

    @GetMapping("/{id}")
    public ShippingAddressResponse getAddress(
            @PathVariable Long id,
            Principal principal) {

        return addressService.getAddress(
                principal.getName(),
                id
        );
    }

    @PostMapping
    public ResponseEntity<ShippingAddressResponse> createAddress(
            @Valid @RequestBody ShippingAddressRequest request,
            Principal principal) {

        ShippingAddressResponse address = addressService.createAddress(
                principal.getName(),
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(address);
    }

    @PutMapping("/{id}")
    public ShippingAddressResponse updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody ShippingAddressRequest request,
            Principal principal) {

        return addressService.updateAddress(
                principal.getName(),
                id,
                request
        );
    }
}