package com.equisibe.service;

import com.equisibe.model.*;
import com.equisibe.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StockReservationServiceTest {

    ProductRepository products = mock(ProductRepository.class);
    EntityManager entityManager = mock(EntityManager.class);
    StockReservationService service =
            new StockReservationService(products, entityManager);

    Product product;
    OrderPayment payment;

    @BeforeEach
    void setUp() {
        product = new Product(
                "Camisa", "Azul", new BigDecimal("40.00"),
                "/images/products/CAMISA.jpeg", "Camisas");
        product.updateStock(ClothingSize.M, 1);

        Order order = mock(Order.class);
        when(order.getStatus()).thenReturn(OrderStatus.PENDING_PAYMENT);
        when(order.getItems()).thenReturn(List.of(
                new OrderItem(5L, "Camisa", ClothingSize.M, 1,
                        new BigDecimal("40.00"))));

        payment = new OrderPayment(order);
        when(products.findLockedById(5L)).thenReturn(Optional.of(product));
    }

    @Test
    void shouldReserveOnlyOnce() {
        service.reserve(payment);
        service.reserve(payment);

        assertEquals(0, product.getStock(ClothingSize.M));
        assertEquals(StockReservationStatus.RESERVED,
                payment.getStockReservationStatus());
        verify(products, times(1)).findLockedById(5L);
    }

    @Test
    void shouldNotDeductAgainWhenPaymentIsConfirmed() {
        service.reserve(payment);
        service.consume(payment);
        service.consume(payment);

        assertEquals(0, product.getStock(ClothingSize.M));
        assertEquals(StockReservationStatus.CONSUMED,
                payment.getStockReservationStatus());
    }

    @Test
    void shouldRestoreOnlyOnceWhenReservationIsReleased() {
        service.reserve(payment);
        service.release(payment);
        service.release(payment);

        assertEquals(1, product.getStock(ClothingSize.M));
        assertEquals(StockReservationStatus.RELEASED,
                payment.getStockReservationStatus());
    }

    @Test
    void shouldRejectInsufficientStockWithoutChangingReservation() {
        product.updateStock(ClothingSize.M, 0);

        assertThrows(ResponseStatusException.class,
                () -> service.reserve(payment));

        assertEquals(0, product.getStock(ClothingSize.M));
        assertEquals(StockReservationStatus.NONE,
                payment.getStockReservationStatus());
    }
}
