package com.equisibe.controller;

import com.equisibe.dto.CheckoutRequest;
import com.equisibe.dto.CheckoutResponse;
import com.equisibe.service.CheckoutService;

import jakarta.validation.Valid;

import java.security.Principal;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/validate")
    public CheckoutResponse validate(
            @Valid @RequestBody CheckoutRequest request,
            Principal principal) {

        return checkoutService.validate(
                principal.getName(),
                request
        );
    }
}