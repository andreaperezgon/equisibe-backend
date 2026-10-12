package com.equisibe.service;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShippingCostServiceTest {

    private final ShippingCostService service =
            new ShippingCostService();

    @ParameterizedTest
    @CsvSource({
            "0.00, 3.95",
            "25.00, 3.95",
            "79.99, 3.95",
            "80.00, 0.00",
            "80.01, 0.00",
            "120.00, 0.00"
    })
    void calculatesShippingCost(String subtotal, String expected) {
        BigDecimal result = service.calculate(
                new BigDecimal(subtotal)
        );

        assertEquals(new BigDecimal(expected), result);
    }

    @Test
    void rejectsNegativeSubtotal() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.calculate(new BigDecimal("-1.00"))
        );
    }

    @Test
    void rejectsMissingSubtotal() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.calculate(null)
        );
    }
}