package com.equisibe.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

@Service
public class ShippingCostService {

    private static final BigDecimal FREE_SHIPPING_THRESHOLD =
            new BigDecimal("80.00");

    private static final BigDecimal STANDARD_SHIPPING_COST =
            new BigDecimal("3.95");

    private static final BigDecimal ZERO =
            new BigDecimal("0.00");

    public BigDecimal calculate(BigDecimal subtotal) {
        if (subtotal == null || subtotal.signum() < 0) {
            throw new IllegalArgumentException(
                    "El subtotal debe ser un importe válido y no negativo."
            );
        }

        if (subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0) {
            return ZERO;
        }

        return STANDARD_SHIPPING_COST;
    }
}