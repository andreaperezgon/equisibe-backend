package com.equisibe.model;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StockReservationModelTest {

    private Product product() {
        Product product = new Product(
                "Camisa", "Camisa azul", new BigDecimal("40.00"),
                "/images/products/CAMISA.jpeg", "Camisas");
        product.updateStock(ClothingSize.M, 2);
        return product;
    }

    private OrderPayment payment() {
        Order order = mock(Order.class);
        when(order.getStatus()).thenReturn(OrderStatus.PENDING_PAYMENT);
        return new OrderPayment(order);
    }

    @Test
    void shouldReserveAndRestoreUnitsOfSameSize() {
        Product product = product();

        product.takeStock(ClothingSize.M, 1);
        assertEquals(1, product.getStock(ClothingSize.M));

        product.restoreStock(ClothingSize.M, 1);
        assertEquals(2, product.getStock(ClothingSize.M));
        assertEquals(0, product.getStock(ClothingSize.S));
    }

    @Test
    void shouldNeverAllowNegativeStock() {
        Product product = product();

        assertThrows(IllegalStateException.class,
                () -> product.takeStock(ClothingSize.M, 3));
        assertEquals(2, product.getStock(ClothingSize.M));

        assertThrows(IllegalArgumentException.class,
                () -> product.takeStock(ClothingSize.M, -1));
        assertThrows(IllegalArgumentException.class,
                () -> product.restoreStock(ClothingSize.M, 0));
    }

    @Test
    void shouldNotReleaseUnitsAlreadySold() {
        OrderPayment payment = payment();

        payment.markStockReserved();
        payment.markStockConsumed();
        payment.markStockConsumed();

        assertEquals(StockReservationStatus.CONSUMED,
                payment.getStockReservationStatus());
        assertThrows(IllegalStateException.class, payment::markStockReleased);
    }

    @Test
    void shouldNotReuseReleasedReservation() {
        OrderPayment payment = payment();

        payment.markStockReserved();
        payment.markStockReleased();
        payment.markStockReleased();

        assertEquals(StockReservationStatus.RELEASED,
                payment.getStockReservationStatus());
        assertThrows(IllegalStateException.class, payment::markStockReserved);
        assertThrows(IllegalStateException.class, payment::markStockConsumed);
    }
}
