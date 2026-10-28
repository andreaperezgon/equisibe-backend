package com.equisibe.service;

import com.equisibe.dto.CartValidationResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class PromotionalDiscountService {
    private final PromotionalCodeService codes;
    private final ShippingCostService shipping;

    public PromotionalDiscountService(
            PromotionalCodeService codes,
            ShippingCostService shipping) {
        this.codes = codes;
        this.shipping = shipping;
    }

    public CartValidationResponse apply(
            CartValidationResponse cart, String value) {
        int percentage = codes.getAvailablePercentage(value);
        if (percentage == 0 || !cart.valid()) return cart;

        String code = value.trim().toUpperCase(Locale.ROOT);
        BigDecimal factor = BigDecimal.valueOf(100 - percentage)
                .movePointLeft(2);

        var items = cart.items().stream().map(item -> {
            BigDecimal unitPrice = item.unitPrice()
                    .multiply(factor)
                    .setScale(2, RoundingMode.HALF_UP);
            return new CartValidationResponse.Item(
                    item.productId(), item.name(), item.size(),
                    item.quantity(), unitPrice,
                    unitPrice.multiply(BigDecimal.valueOf(item.quantity()))
            );
        }).toList();

        BigDecimal subtotal = items.stream()
                .map(item -> item.lineTotal())
                .reduce(new BigDecimal("0.00"), (runningTotal, lineTotal) -> runningTotal.add(lineTotal));
        BigDecimal shippingCost = shipping.calculate(subtotal);

        return new CartValidationResponse(
                true, items, cart.issues(), subtotal,
                shippingCost, subtotal.add(shippingCost),
                cart.currency(), code,
                cart.subtotal().subtract(subtotal),
                cart.subtotal()
        );
    }
}
