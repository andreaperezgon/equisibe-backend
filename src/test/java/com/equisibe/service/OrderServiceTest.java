package com.equisibe.service;

import com.equisibe.dto.CartValidationRequest;
import com.equisibe.dto.CartValidationResponse;
import com.equisibe.dto.CheckoutRequest;
import com.equisibe.dto.CheckoutResponse;
import com.equisibe.dto.ShippingAddressResponse;
import com.equisibe.model.ClothingSize;
import com.equisibe.model.Order;
import com.equisibe.model.OrderStatus;
import com.equisibe.model.User;
import com.equisibe.repository.OrderRepository;
import com.equisibe.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final String EMAIL = "cliente@example.com";

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CheckoutService checkoutService;

    private OrderService service;
    private User user;
    private CheckoutRequest request;

    @BeforeEach
    void setUp() {
        service = new OrderService(
                orderRepository,
                userRepository,
                checkoutService
        );

        user = new User("Cliente", EMAIL, "password-test");

        request = new CheckoutRequest(
                10L,
                List.of(new CartValidationRequest.Item(
                        1L,
                        ClothingSize.L,
                        1
                ))
        );
    }

    @Test
    void savesPendingOrderWithValidatedPricesAndAddress() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        var item = new CartValidationResponse.Item(
                1L,
                "Sudadera",
                ClothingSize.L,
                1L,
                new BigDecimal("54.90"),
                new BigDecimal("54.90")
        );

        var cart = new CartValidationResponse(
                true,
                List.of(item),
                List.of(),
                new BigDecimal("54.90"),
                new BigDecimal("3.95"),
                new BigDecimal("58.85"),
                "EUR"
        );

        when(checkoutService.validate(EMAIL, request))
                .thenReturn(new CheckoutResponse(address(), cart));

        Order savedOrder = mock(Order.class);
        when(savedOrder.getId()).thenReturn(42L);
        when(orderRepository.save(any(Order.class)))
                .thenReturn(savedOrder);

        Long id = service.createPendingOrder(EMAIL, request);

        ArgumentCaptor<Order> captor =
                ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());

        Order order = captor.getValue();

        assertEquals(Long.valueOf(42L), id);
        assertSame(user, order.getUser());
        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
        assertEquals("EUR", order.getCurrency());
        assertNotNull(order.getCreatedAt());

        assertEquals(new BigDecimal("54.90"), order.getSubtotal());
        assertEquals(new BigDecimal("3.95"), order.getShippingCost());
        assertEquals(new BigDecimal("58.85"), order.getTotal());

        assertEquals(1, order.getItems().size());
        var line = order.getItems().getFirst();
        assertEquals(Long.valueOf(1L), line.getProductId());
        assertEquals("Sudadera", line.getProductName());
        assertEquals(ClothingSize.L, line.getSize());
        assertEquals(1L, line.getQuantity());
        assertEquals(new BigDecimal("54.90"), line.getUnitPrice());

        var shipping = order.getShippingAddress();
        assertEquals("Cliente Prueba", shipping.getRecipientName());
        assertEquals("600123456", shipping.getPhone());
        assertEquals("Calle Ejemplo 1", shipping.getAddressLine1());
        assertEquals("2B", shipping.getAddressLine2());
        assertEquals("Oviedo", shipping.getCity());
        assertEquals("Asturias", shipping.getProvince());
        assertEquals("33001", shipping.getPostalCode());
        assertEquals("ES", shipping.getCountryCode());
    }

    @Test
    void doesNotSaveWhenCartIsInvalid() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        var cart = new CartValidationResponse(
                false,
                List.of(),
                List.of(),
                null,
                null,
                null,
                "EUR"
        );

        when(checkoutService.validate(EMAIL, request))
                .thenReturn(new CheckoutResponse(address(), cart));

        var error = assertThrows(
                ResponseStatusException.class,
                () -> service.createPendingOrder(EMAIL, request)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verifyNoInteractions(orderRepository);
    }

    @Test
    void doesNotSaveWhenCheckoutRejectsAddress() {
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(checkoutService.validate(EMAIL, request))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "La dirección no está disponible."
                ));

        var error = assertThrows(
                ResponseStatusException.class,
                () -> service.createPendingOrder(EMAIL, request)
        );

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(orderRepository);
    }

    @Test
    void rejectsMissingSessionBeforeCheckingCheckout() {
        var error = assertThrows(
                ResponseStatusException.class,
                () -> service.createPendingOrder(null, request)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
        verifyNoInteractions(
                userRepository,
                checkoutService,
                orderRepository
        );
    }

    private ShippingAddressResponse address() {
        return new ShippingAddressResponse(
                10L,
                "Cliente Prueba",
                "600123456",
                "Calle Ejemplo 1",
                "2B",
                "Oviedo",
                "Asturias",
                "33001",
                "ES"
        );
    }
}