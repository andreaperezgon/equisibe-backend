package com.equisibe.controller;

import com.equisibe.dto.AdminOrderResponse;
import com.equisibe.model.OrderStatus;
import com.equisibe.service.AdminOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final AdminOrderService orderService;

    public AdminOrderController(AdminOrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<AdminOrderResponse> getOrders(Principal principal) {
        return orderService.getOrders(requireEmail(principal));
    }

    @GetMapping("/{orderId}")
    public AdminOrderResponse getOrder(
            @PathVariable("orderId") Long orderId,
            Principal principal) {
        return orderService.getOrder(requireEmail(principal), orderId);
    }

    @PutMapping("/{orderId}/status")
    public AdminOrderResponse updateStatus(
            @PathVariable("orderId") Long orderId,
            @Valid @RequestBody StatusRequest request,
            Principal principal) {
        return orderService.updateStatus(
                requireEmail(principal), orderId, request.status()
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

    public record StatusRequest(
            @NotNull(message = "Selecciona un estado.")
            OrderStatus status
    ) {
    }
}
