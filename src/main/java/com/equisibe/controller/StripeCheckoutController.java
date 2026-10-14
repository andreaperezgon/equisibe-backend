package com.equisibe.controller;

import com.equisibe.service.StripeCheckoutService;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class StripeCheckoutController {
    private final StripeCheckoutService checkoutService;

    public StripeCheckoutController(StripeCheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/api/orders/{orderId}/checkout-session")
    public StripeCheckoutService.CheckoutSessionResponse createSession(
            @PathVariable("orderId") Long orderId, Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Debes iniciar sesión.");
        }
        if (orderId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El identificador del pedido no es válido.");
        }
        return checkoutService.createSession(principal.getName(), orderId);
    }
}
