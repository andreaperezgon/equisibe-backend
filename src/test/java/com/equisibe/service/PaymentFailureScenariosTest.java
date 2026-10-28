package com.equisibe.service;

import com.equisibe.model.ClothingSize;
import com.equisibe.model.Order;
import com.equisibe.model.OrderItem;
import com.equisibe.model.OrderPayment;
import com.equisibe.model.OrderShippingAddress;
import com.equisibe.model.OrderStatus;
import com.equisibe.model.User;
import com.equisibe.repository.OrderPaymentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentFailureScenariosTest {
    final OrderPaymentRepository repository = mock(OrderPaymentRepository.class);
    final EntityManager entityManager = mock(EntityManager.class);
    final StockReservationService stock = mock(StockReservationService.class);
    final OrderPaymentService service = new OrderPaymentService(repository, entityManager, stock);
    final Instant expires = Instant.parse("2027-01-01T00:00:00Z");
    Order order;
    OrderPayment payment;

    @BeforeEach void setup() {
        order = new Order(new User("Cliente", "client@example.com", "encoded"),
                new OrderShippingAddress("Cliente", "600000000", "Calle 1", null,
                        "Oviedo", "Asturias", "33001", "ES"),
                List.of(new OrderItem(1L, "Camisa", ClothingSize.S, 1, new BigDecimal("40.00"))),
                new BigDecimal("3.95"));
        payment = new OrderPayment(order);
        when(entityManager.find(Order.class, 1L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(order);
        when(repository.findByOrder_Id(1L)).thenReturn(Optional.of(payment));
    }
    void error(int status, org.junit.jupiter.api.function.Executable action) {
        assertEquals(status, assertThrows(ResponseStatusException.class, action).getStatusCode().value());
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={" "})
    void requiresSessionBeforePreparingOrAttaching(String email) {
        error(401, () -> service.prepare(email, 1L));
        error(401, () -> service.attachSession(email, 1L, "cs_test_a", expires));
        verifyNoInteractions(stock);
    }
    @ParameterizedTest @NullSource @ValueSource(longs={0,-1})
    void rejectsInvalidOrderIds(Long id) {
        error(400, () -> service.prepare("client@example.com", id));
        error(400, () -> service.confirmPayment(id, "cs_test_a", 4395L, "eur"));
        error(400, () -> service.expirePayment(id, "cs_test_a", expires));
    }
    @Test void rejectsMissingOrderAndMissingPreparedPayment() {
        when(entityManager.find(Order.class, 1L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(null);
        error(404, () -> service.confirmPayment(1L, "cs_test_a", 4395L, "eur"));
        error(404, () -> service.expirePayment(1L, "cs_test_a", expires));
        when(entityManager.find(Order.class, 1L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(order);
        when(repository.findByOrder_Id(1L)).thenReturn(Optional.empty());
        error(409, () -> service.confirmPayment(1L, "cs_test_a", 4395L, "eur"));
        error(409, () -> service.expirePayment(1L, "cs_test_a", expires));
    }
    @ParameterizedTest @NullSource @ValueSource(strings={"cs_test_other"})
    void rejectsMismatchedSession(String sessionId) {
        payment.attachSession("cs_test_expected", expires);
        error(409, () -> service.confirmPayment(1L, sessionId, 4395L, "eur"));
        verifyNoInteractions(stock);
        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
    }
    @ParameterizedTest @NullSource @ValueSource(longs={0,4394,4396})
    void rejectsIncorrectAmount(Long amount) {
        payment.attachSession("cs_test_a", expires);
        error(409, () -> service.confirmPayment(1L, "cs_test_a", amount, "eur"));
        verifyNoInteractions(stock);
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={"usd"})
    void rejectsIncorrectCurrency(String currency) {
        payment.attachSession("cs_test_a", expires);
        error(409, () -> service.confirmPayment(1L, "cs_test_a", 4395L, currency));
        verifyNoInteractions(stock);
    }
    @Test void rejectsConfirmingExpiredOrder() {
        payment.attachSession("cs_test_a", expires);
        order.markExpired();
        error(409, () -> service.confirmPayment(1L, "cs_test_a", 4395L, "eur"));
        verifyNoInteractions(stock);
    }
    @Test void confirmsLegacyPaymentReservingStockOnceAndIgnoresDuplicateConfirmation() {
        payment.attachSession("cs_test_a", expires);
        service.confirmPayment(1L, "cs_test_a", 4395L, "EUR");
        assertEquals(OrderStatus.PAID, order.getStatus());
        verify(stock).reserve(payment);
        verify(stock).consume(payment);
        service.confirmPayment(1L, "cs_test_a", 4395L, "eur");
        verifyNoMoreInteractions(stock);
    }
    @Test void attachesMissingSessionOnExpiryAndDoesNotReleaseTwice() {
        service.expirePayment(1L, "cs_test_a", expires);
        assertEquals("cs_test_a", payment.getStripeSessionId());
        assertEquals(OrderStatus.EXPIRED, order.getStatus());
        verify(stock).release(payment);
        service.expirePayment(1L, "cs_test_a", expires);
        verifyNoMoreInteractions(stock);
    }
    @Test void doesNotExpirePaidOrdersOrAcceptDifferentSession() {
        payment.attachSession("cs_test_a", expires);
        error(409, () -> service.expirePayment(1L, "cs_test_other", expires));
        order.markPaid();
        service.expirePayment(1L, "cs_test_a", expires);
        assertEquals(OrderStatus.PAID, order.getStatus());
        verifyNoInteractions(stock);
    }
    @Test void requiresExpirySessionAndDate() {
        error(400, () -> service.expirePayment(1L, null, expires));
        error(400, () -> service.expirePayment(1L, "cs_test_a", null));
    }
    @Test void reconciliationIgnoresMissingPaymentAndLocksExistingPayment() {
        when(repository.findById(7L)).thenReturn(Optional.empty());
        service.markStockReconciled(7L);
        verify(entityManager, never()).lock(any(), any());
        when(repository.findById(8L)).thenReturn(Optional.of(payment));
        service.markStockReconciled(8L);
        verify(entityManager).lock(order, LockModeType.PESSIMISTIC_WRITE);
        verify(entityManager).refresh(payment, LockModeType.PESSIMISTIC_WRITE);
        verify(entityManager).flush();
    }
    @Test @SuppressWarnings("unchecked")
    void missingOwnedOrderAndMissingPreparedSessionAreRejected() {
        TypedQuery<Order> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Order.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.setLockMode(any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());
        error(404, () -> service.prepare("client@example.com", 1L));
        when(query.getResultList()).thenReturn(List.of(order));
        when(repository.findByOrder_Id(1L)).thenReturn(Optional.empty());
        error(409, () -> service.attachSession("client@example.com", 1L, "cs_test_a", expires));
    }
}
