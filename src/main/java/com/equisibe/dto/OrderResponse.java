package com.equisibe.dto;

import com.equisibe.model.ClothingSize;
import com.equisibe.model.Order;
import com.equisibe.model.OrderShippingAddress;
import com.equisibe.model.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        OrderStatus status,
        Instant createdAt,
        Address shippingAddress,
        List<Item> items,
        BigDecimal subtotal,
        BigDecimal shippingCost,
        BigDecimal total,
        String currency
) {

    public static OrderResponse from(Order order) {
        List<Item> items = order.getItems().stream()
                .map(item -> new Item(
                        item.getProductId(),
                        item.getProductName(),
                        item.getSize(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getLineTotal()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getCreatedAt(),
                Address.from(order.getShippingAddress()),
                items,
                order.getSubtotal(),
                order.getShippingCost(),
                order.getTotal(),
                order.getCurrency()
        );
    }

    public record Item(
            Long productId,
            String productName,
            ClothingSize size,
            long quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {
    }

    public record Address(
            String recipientName,
            String phone,
            String addressLine1,
            String addressLine2,
            String city,
            String province,
            String postalCode,
            String countryCode
    ) {

        public static Address from(OrderShippingAddress address) {
            return new Address(
                    address.getRecipientName(),
                    address.getPhone(),
                    address.getAddressLine1(),
                    address.getAddressLine2(),
                    address.getCity(),
                    address.getProvince(),
                    address.getPostalCode(),
                    address.getCountryCode()
            );
        }
    }
}