package com.equisibe.controller;

import com.equisibe.dto.CartValidationRequest;
import com.equisibe.dto.CartValidationResponse;
import com.equisibe.service.CartValidationService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartValidationService cartValidationService;

    public CartController(CartValidationService cartValidationService) {
        this.cartValidationService = cartValidationService;
    }

    @PostMapping("/validate")
    public CartValidationResponse validate(
            @Valid @RequestBody CartValidationRequest request) {

        return cartValidationService.validate(request);
    }
}