package com.equisibe.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Embedded
    private OrderShippingAddress shippingAddress;

    @ElementCollection
    @CollectionTable(
            name = "order_items",
            joinColumns = @JoinColumn(name = "order_id")
    )
    @OrderColumn(name = "line_position")
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "shipping_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal shippingCost;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, length = 3)
    private String currency = "EUR";

    protected Order() {
    }

    public Order(
            User user,
            OrderShippingAddress shippingAddress,
            List<OrderItem> items,
            BigDecimal shippingCost) {

        this.user = Objects.requireNonNull(
                user,
                "El cliente es obligatorio."
        );
        this.shippingAddress = Objects.requireNonNull(
                shippingAddress,
                "La dirección de envío es obligatoria."
        );

        Objects.requireNonNull(items, "Las prendas son obligatorias.");

        if (items.isEmpty()) {
            throw new IllegalArgumentException(
                    "El pedido debe contener al menos una prenda."
            );
        }

        this.items = new ArrayList<>(List.copyOf(items));
        this.subtotal = validateAmount(
                this.items.stream()
                        .map(OrderItem::getLineTotal)
                        .reduce(new BigDecimal("0.00"), BigDecimal::add)
        );
        this.shippingCost = validateAmount(shippingCost);
        this.total = validateAmount(this.subtotal.add(this.shippingCost));
        this.createdAt = Instant.now();
    }

    private static BigDecimal validateAmount(BigDecimal amount) {
        Objects.requireNonNull(amount, "El importe es obligatorio.");

        if (amount.signum() < 0) {
            throw new IllegalArgumentException(
                    "El importe no puede ser negativo."
            );
        }

        BigDecimal normalized = amount.setScale(
                2,
                RoundingMode.UNNECESSARY
        );

        if (normalized.precision() > 19) {
            throw new IllegalArgumentException(
                    "El importe supera el máximo admitido."
            );
        }

        return normalized;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public OrderShippingAddress getShippingAddress() {
        return shippingAddress;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getShippingCost() {
        return shippingCost;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getCurrency() {
        return currency;
    }
}