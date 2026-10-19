package com.equisibe.model;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProductSaleTest {
    private Product product() {
        return new Product(
                "Sudadera", "Descripción",
                new BigDecimal("54.90"),
                "/images/products/sudadera.jpg", "Sudaderas"
        );
    }

    @Test
    void appliesSaleAndRestoresRegularPrice() {
        var product = product();
        product.updateSalePrice(new BigDecimal("39.90"));
        assertEquals(new BigDecimal("39.90"), product.getEffectivePrice());
        assertEquals(new BigDecimal("54.90"), product.getPrice());

        product.updateSalePrice(null);
        assertNull(product.getSalePrice());
        assertEquals(new BigDecimal("54.90"), product.getEffectivePrice());
    }

    @Test
    void rejectsInvalidSalePrices() {
        var product = product();
        for (String value : new String[]{"0", "-1", "54.90", "60"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> product.updateSalePrice(new BigDecimal(value)));
        }
    }

    @Test
    void removesSaleWhenRegularPriceDropsBelowIt() {
        var product = product();
        product.updateSalePrice(new BigDecimal("39.90"));
        product.updateDetails(
                "Sudadera", "Descripción", new BigDecimal("29.90"),
                "/images/products/sudadera.jpg", "Sudaderas"
        );
        assertNull(product.getSalePrice());
        assertEquals(new BigDecimal("29.90"), product.getEffectivePrice());
    }
}
