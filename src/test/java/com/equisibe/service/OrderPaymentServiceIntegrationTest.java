package com.equisibe.service;

import com.equisibe.model.ClothingSize;
import com.equisibe.model.Order;
import com.equisibe.model.OrderItem;
import com.equisibe.model.OrderShippingAddress;
import com.equisibe.model.User;
import com.equisibe.repository.OrderPaymentRepository;
import com.equisibe.repository.OrderRepository;
import com.equisibe.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderPaymentServiceIntegrationTest {

    @Autowired
    private OrderPaymentService service;

    @Autowired
    private OrderPaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private String email;
    private Long orderId;

    @BeforeEach
    void setUp() {
        email = UUID.randomUUID() + "@example.com";

        User customer = userRepository.saveAndFlush(new User(
                "Cliente de prueba",
                email,
                "test-password"
        ));

        var address = new OrderShippingAddress(
                "Cliente de prueba",
                "600123456",
                "Calle Mayor 10",
                null,
                "Oviedo",
                "Asturias",
                "33001",
                "ES"
        );

        var item = new OrderItem(
                1L,
                "Camiseta",
                ClothingSize.S,
                1L,
                new BigDecimal("40.00")
        );

        Order order = orderRepository.saveAndFlush(new Order(
                customer,
                address,
                List.of(item),
                new BigDecimal("3.95")
        ));

        orderId = order.getId();
        entityManager.clear();
    }

    @Test
    void reusesPaymentWhenPreparingAgain() {
        long countBefore = paymentRepository.count();

        var first = service.prepare(email, orderId);
        entityManager.clear();

        var second = service.prepare(email, orderId);

        assertEquals(countBefore + 1, paymentRepository.count());
        assertEquals(orderId, first.order().id());
        assertEquals(first.idempotencyKey(), second.idempotencyKey());
        assertTrue(first.idempotencyKey().startsWith("checkout-"));
entityManager.clear();
var reloaded = service.prepare(email, orderId);

assertEquals(second.createdAt(), reloaded.createdAt());        assertNull(second.sessionId());
    }

    @Test
    void persistsSessionAndAcceptsRepeatedAttachment() {
        var prepared = service.prepare(email, orderId);
        Instant expiresAt = Instant.parse("2026-10-15T12:00:00Z");

        service.attachSession(
                email, orderId, "cs_test_example", expiresAt
        );
        entityManager.clear();

        service.attachSession(
                email, orderId, "cs_test_example", expiresAt
        );
        entityManager.clear();

        var stored = service.prepare(email, orderId);

        assertEquals("cs_test_example", stored.sessionId());
        assertEquals(expiresAt, stored.sessionExpiresAt());
        assertEquals(prepared.idempotencyKey(), stored.idempotencyKey());
    }

    @Test
    void rejectsAnotherCustomersOrderWithoutCreatingPayment() {
        long countBefore = paymentRepository.count();

        var error = assertThrows(
                ResponseStatusException.class,
                () -> service.prepare("other@example.com", orderId)
        );

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        assertEquals(countBefore, paymentRepository.count());
    }

    @Test
    void rejectsAttachingSessionWithoutPreparingPayment() {
        var error = assertThrows(
                ResponseStatusException.class,
                () -> service.attachSession(
                        email,
                        orderId,
                        "cs_test_example",
                        Instant.parse("2026-10-15T12:00:00Z")
                )
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertTrue(paymentRepository.findByOrder_Id(orderId).isEmpty());
    }

    @Test
    void rejectsMissingAuthentication() {
        var error = assertThrows(
                ResponseStatusException.class,
                () -> service.prepare(null, orderId)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
    }

    @Test
    void rejectsInvalidOrderId() {
        var error = assertThrows(
                ResponseStatusException.class,
                () -> service.prepare(email, 0L)
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }
}