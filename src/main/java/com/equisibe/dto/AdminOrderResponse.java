package com.equisibe.dto;

import com.equisibe.model.Order;
import com.equisibe.model.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AdminOrderResponse(
        Long id,
        OrderStatus status,
        Instant createdAt,
        String customerName,
        String customerEmail,
        OrderResponse.Address shippingAddress,
        List<OrderResponse.Item> items,
        BigDecimal subtotal,
        BigDecimal shippingCost,
        BigDecimal total,
        String currency
) {

    public static AdminOrderResponse from(Order order) {
        var details = OrderResponse.from(order);

        return new AdminOrderResponse(
                details.id(),
                details.status(),
                details.createdAt(),
                order.getUser().getName(),
                order.getUser().getEmail(),
                details.shippingAddress(),
                details.items(),
                details.subtotal(),
                details.shippingCost(),
                details.total(),
                details.currency()
        );
    }
}
