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

    public OrderPaymentService(
            OrderPaymentRepository paymentRepository,
            EntityManager entityManager) {

        this.paymentRepository = paymentRepository;
        this.entityManager = entityManager;
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