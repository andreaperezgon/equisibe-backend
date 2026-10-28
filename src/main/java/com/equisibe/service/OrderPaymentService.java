package com.equisibe.service;

import com.equisibe.dto.OrderResponse;
import com.equisibe.model.Order;
import com.equisibe.model.OrderPayment;
import com.equisibe.model.OrderStatus;
import com.equisibe.repository.OrderPaymentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderPaymentService {

    private final OrderPaymentRepository paymentRepository;
    private final EntityManager entityManager;
    private final StockReservationService stockReservations;

    public OrderPaymentService(
            OrderPaymentRepository paymentRepository,
            EntityManager entityManager,
            StockReservationService stockReservations) {

        this.paymentRepository = paymentRepository;
        this.entityManager = entityManager;
        this.stockReservations = stockReservations;
    }

    @Transactional
    public PreparedPayment prepare(String email, Long orderId) {
        Order order = getOwnedOrderWithLock(email, orderId);

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El pedido no está pendiente de pago."
            );
        }

        OrderPayment payment = paymentRepository
                .findByOrder_Id(orderId)
                .orElseGet(() -> paymentRepository.saveAndFlush(
                        new OrderPayment(order)
                ));

        stockReservations.reserve(payment);

        return new PreparedPayment(
                OrderResponse.from(order),
                payment.getIdempotencyKey(),
                payment.getStripeSessionId(),
                payment.getSessionExpiresAt(),
                payment.getCreatedAt()
        );
    }

    @Transactional
    public void attachSession(
            String email,
            Long orderId,
            String sessionId,
            Instant expiresAt) {

        getOwnedOrderWithLock(email, orderId);

        OrderPayment payment = paymentRepository
                .findByOrder_Id(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Primero debes preparar el pago del pedido."
                ));

        payment.attachSession(sessionId, expiresAt);
        paymentRepository.saveAndFlush(payment);
    }

    @Transactional
    public void confirmPayment(
            Long orderId,
            String sessionId,
            Long amountTotal,
            String currency) {

        if (orderId == null || orderId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador del pedido no es válido."
            );
        }

        Order order = entityManager.find(
                Order.class,
                orderId,
                LockModeType.PESSIMISTIC_WRITE
        );

        if (order == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "El pedido no está disponible."
            );
        }

        OrderPayment payment = paymentRepository
                .findByOrder_Id(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "El pedido no tiene un pago preparado."
                ));

        if (sessionId == null
                || !sessionId.equals(payment.getStripeSessionId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La sesión de pago no coincide con el pedido."
            );
        }

        long expectedAmount = order.getTotal()
                .movePointRight(2)
                .longValueExact();

        if (amountTotal == null
                || amountTotal.longValue() != expectedAmount
                || !order.getCurrency().equalsIgnoreCase(currency)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El importe o la moneda no coinciden con el pedido."
            );
        }

        switch (order.getStatus()) {
            case PENDING_PAYMENT -> {
                if (payment.getStockReservationStatus()
                        == com.equisibe.model.StockReservationStatus.NONE) {
                    stockReservations.reserve(payment);
                }
                stockReservations.consume(payment);
                order.markPaid();
            }
            case PAID, PREPARING, SHIPPED, DELIVERED -> {
                // Una confirmación repetida no modifica el estado.
            }
            default -> throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El estado del pedido no permite confirmar el pago."
            );
        }
    }


    @Transactional
    public void expirePayment(Long orderId, String sessionId, Instant expiresAt) {
        if (orderId == null || orderId <= 0 || sessionId == null || expiresAt == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        Order order = entityManager.find(
                Order.class, orderId, LockModeType.PESSIMISTIC_WRITE);
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        OrderPayment payment = paymentRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT));

        if (payment.getStripeSessionId() == null) {
            payment.attachSession(sessionId, expiresAt);
        }

        if (!sessionId.equals(payment.getStripeSessionId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "La sesión no coincide con el pedido.");
        }

        if (order.getStatus() == OrderStatus.EXPIRED) return;
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) return;

        stockReservations.release(payment);
        order.markExpired();
        entityManager.flush();
    }


    @Transactional
    public void markStockReconciled(Long paymentId) {
        var payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) return;

        entityManager.lock(payment.getOrder(), LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(payment, LockModeType.PESSIMISTIC_WRITE);
        payment.markReconciled();
        entityManager.flush();
    }

    private Order getOwnedOrderWithLock(String email, Long orderId) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Debes iniciar sesión."
            );
        }

        if (orderId == null || orderId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador del pedido no es válido."
            );
        }

        return entityManager.createQuery(
                        """
                        select o
                        from Order o
                        join o.user u
                        where o.id = :orderId
                          and u.email = :email
                          and u.enabled = true
                        """,
                        Order.class
                )
                .setParameter("orderId", orderId)
                .setParameter("email", email)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList()
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El pedido no está disponible."
                ));
    }

    public record PreparedPayment(
            OrderResponse order,
            String idempotencyKey,
            String sessionId,
            Instant sessionExpiresAt,
            Instant createdAt
    ) {
    }
}