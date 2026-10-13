package com.equisibe.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "order_payments")
public class OrderPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "order_id",
            nullable = false,
            unique = true,
            updatable = false
    )
    private Order order;

    @Column(
            name = "idempotency_key",
            nullable = false,
            unique = true,
            updatable = false,
            length = 100
    )
    private String idempotencyKey;

    @Column(name = "stripe_session_id", unique = true, length = 255)
    private String stripeSessionId;

    @Column(name = "session_expires_at")
    private Instant sessionExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OrderPayment() {
    }

    public OrderPayment(Order order) {
        this.order = Objects.requireNonNull(
                order,
                "El pedido es obligatorio."
        );

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalArgumentException(
                    "El pedido debe estar pendiente de pago."
            );
        }

        this.idempotencyKey = "checkout-" + UUID.randomUUID();
        this.createdAt = Instant.now();
    }

    public void attachSession(String sessionId, Instant expiresAt) {
        if (sessionId == null
                || !sessionId.startsWith("cs_test_")
                || sessionId.length() > 255) {
            throw new IllegalArgumentException(
                    "La sesión de Stripe debe ser una sesión de prueba."
            );
        }

        Objects.requireNonNull(
                expiresAt,
                "La caducidad de la sesión es obligatoria."
        );

        if (stripeSessionId != null) {
            if (!stripeSessionId.equals(sessionId)
                    || !sessionExpiresAt.equals(expiresAt)) {
                throw new IllegalStateException(
                        "El pedido ya tiene otra sesión de pago asociada."
                );
            }

            return;
        }

        this.stripeSessionId = sessionId;
        this.sessionExpiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getStripeSessionId() {
        return stripeSessionId;
    }

    public Instant getSessionExpiresAt() {
        return sessionExpiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}