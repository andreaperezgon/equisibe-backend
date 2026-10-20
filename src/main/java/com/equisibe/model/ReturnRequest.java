package com.equisibe.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "return_requests")
public class ReturnRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false, unique = true)
    private UUID clientRequestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReturnType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReturnStatus status = ReturnStatus.REQUESTED;

    @Column(nullable = false, length = 2000)
    private String note = "";

    @Column(nullable = false, length = 2000)
    private String adminNote = "";

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @ElementCollection
    @CollectionTable(
            name = "return_request_items",
            joinColumns = @JoinColumn(name = "return_request_id")
    )
    @OrderColumn(name = "position")
    private List<ReturnItem> items = new ArrayList<>();

    protected ReturnRequest() {
    }

    public ReturnRequest(
            UUID clientRequestId,
            Order order,
            ReturnType type,
            String note,
            List<ReturnItem> items) {
        this.clientRequestId = Objects.requireNonNull(clientRequestId);
        this.order = Objects.requireNonNull(order);
        this.type = Objects.requireNonNull(type);
        this.note = note == null ? "" : note.trim();
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Selecciona alguna prenda.");
        }
        this.items = new ArrayList<>(items);
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }


    public void updateStatus(ReturnStatus next, String response) {
        Objects.requireNonNull(next);
        String message = response == null ? "" : response.trim();

        if ((next == ReturnStatus.REJECTED || next == ReturnStatus.CLOSED)
                && message.isBlank()) {
            throw new IllegalArgumentException(
                    "Explica el motivo o cómo se ha resuelto la solicitud."
            );
        }

        boolean allowed = status == next || switch (status) {
            case REQUESTED -> next == ReturnStatus.IN_REVIEW
                    || next == ReturnStatus.AWAITING_ITEMS
                    || next == ReturnStatus.REJECTED;
            case IN_REVIEW -> next == ReturnStatus.AWAITING_ITEMS
                    || next == ReturnStatus.REJECTED;
            case AWAITING_ITEMS -> next == ReturnStatus.RECEIVED;
            case RECEIVED -> next == ReturnStatus.CLOSED;
            default -> false;
        };

        if (!allowed) {
            throw new IllegalStateException(
                    "La solicitud no puede pasar a ese estado."
            );
        }

        status = next;
        adminNote = message;
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public UUID getClientRequestId() { return clientRequestId; }
    public Order getOrder() { return order; }
    public ReturnType getType() { return type; }
    public ReturnStatus getStatus() { return status; }
    public String getNote() { return note; }
    public String getAdminNote() { return adminNote; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public List<ReturnItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
