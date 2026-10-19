package com.equisibe.dto;

import com.equisibe.model.ClothingSize;

import java.math.BigDecimal;
import java.util.List;

public record CartValidationResponse(
        boolean valid,
        List<Item> items,
        List<Issue> issues,
        BigDecimal subtotal,
        BigDecimal shippingCost,
        BigDecimal total,
        String currency,
        String promoCode,
        BigDecimal discountAmount,
        BigDecimal originalSubtotal
) {
    public CartValidationResponse(
            boolean valid,
            List<Item> items,
            List<Issue> issues,
            BigDecimal subtotal,
            BigDecimal shippingCost,
            BigDecimal total,
            String currency) {
        this(valid, items, issues, subtotal, shippingCost, total,
                currency, null, new BigDecimal("0.00"), subtotal);
    }


    public record Item(
            Long productId,
            String name,
            ClothingSize size,
            long quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {
    }

    public record Issue(
            Long productId,
            ClothingSize size,
            String code,
            String message,
            Integer availableStock
    ) {
    }
}