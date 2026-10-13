package com.equisibe.controller;

import com.equisibe.dto.CheckoutRequest;
import com.equisibe.dto.OrderResponse;
import com.equisibe.service.OrderService;
import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<CreatedOrderResponse> createOrder(
            @Valid @RequestBody CheckoutRequest request,
            Principal principal) {

        Long orderId = orderService.createPendingOrder(
                requireEmail(principal),
                request
        );

        return ResponseEntity
                .created(URI.create("/api/orders/" + orderId))
                .body(new CreatedOrderResponse(orderId));
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(
            @PathVariable("orderId") Long orderId,
            Principal principal) {

        return orderService.getOrder(
                requireEmail(principal),
                orderId
        );
    }

    private String requireEmail(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Debes iniciar sesión."
            );
        }

        return principal.getName();
    }

    public record CreatedOrderResponse(Long id) {
    }
}