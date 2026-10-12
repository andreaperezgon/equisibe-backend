package com.equisibe.service;

import com.equisibe.dto.CartValidationRequest;
import com.equisibe.model.ClothingSize;
import com.equisibe.model.Product;
import com.equisibe.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartShippingTotalsTest {

    @Mock
    private ProductRepository productRepository;

    private CartValidationService service;

    @BeforeEach
    void setUp() {
        service = new CartValidationService(
                productRepository,
                new ShippingCostService()
        );
    }

    @ParameterizedTest
    @CsvSource({
            "25.00, 1, 25.00, 3.95, 28.95",
            "79.99, 1, 79.99, 3.95, 83.94",
            "40.00, 2, 80.00, 0.00, 80.00",
            "80.00, 1, 80.00, 0.00, 80.00",
            "80.01, 1, 80.01, 0.00, 80.01"
    })
    void calculatesShippingAndTotal(
            String price,
            int quantity,
            String expectedSubtotal,
            String expectedShipping,
            String expectedTotal) {

        var product = productWithStock(price, 5);

        when(productRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(product));

        var result = service.validate(request(quantity));

        assertTrue(result.valid());
        assertEquals(new BigDecimal(expectedSubtotal), result.subtotal());
        assertEquals(new BigDecimal(expectedShipping), result.shippingCost());
        assertEquals(new BigDecimal(expectedTotal), result.total());
        assertEquals("EUR", result.currency());
    }

    @Test
    void appliesFreeShippingToTheCombinedSubtotal() {
        when(productRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(productWithStock("45.00", 2)));

        when(productRepository.findByIdAndActiveTrue(2L))
                .thenReturn(Optional.of(productWithStock("35.00", 2)));

        var request = new CartValidationRequest(List.of(
                new CartValidationRequest.Item(1L, ClothingSize.S, 1),
                new CartValidationRequest.Item(2L, ClothingSize.S, 1)
        ));

        var result = service.validate(request);

        assertTrue(result.valid());
        assertEquals(new BigDecimal("80.00"), result.subtotal());
        assertEquals(new BigDecimal("0.00"), result.shippingCost());
        assertEquals(new BigDecimal("80.00"), result.total());
    }

    @Test
    void doesNotReturnTotalsWhenStockIsInsufficient() {
        when(productRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(productWithStock("45.00", 1)));

        var result = service.validate(request(2));

        assertFalse(result.valid());
        assertNull(result.subtotal());
        assertNull(result.shippingCost());
        assertNull(result.total());
    }

    @Test
    void doesNotReturnTotalsWhenProductIsUnavailable() {
        when(productRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.empty());

        var result = service.validate(request(1));

        assertFalse(result.valid());
        assertNull(result.subtotal());
        assertNull(result.shippingCost());
        assertNull(result.total());
    }

    private CartValidationRequest request(int quantity) {
        return new CartValidationRequest(List.of(
                new CartValidationRequest.Item(
                        1L,
                        ClothingSize.S,
                        quantity
                )
        ));
    }

    private Product productWithStock(String price, int stock) {
        var product = new Product(
                "Prenda de prueba",
                "Descripción",
                new BigDecimal(price),
                "/images/prueba.jpg",
                "Camisetas"
        );

        product.updateStock(ClothingSize.S, stock);
        return product;
    }
}