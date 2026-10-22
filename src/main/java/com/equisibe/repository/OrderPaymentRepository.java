package com.equisibe.repository;

import com.equisibe.model.OrderPayment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderPaymentRepository
        extends JpaRepository<OrderPayment, Long> {

    Optional<OrderPayment> findByOrder_Id(Long orderId);

    Optional<OrderPayment> findByStripeSessionId(String stripeSessionId);

    @org.springframework.data.jpa.repository.Query("""
            select p.id as paymentId,
                   p.order.id as orderId,
                   p.order.user.email as email,
                   p.stripeSessionId as sessionId
            from OrderPayment p
            where p.stockReservationStatus =
                  com.equisibe.model.StockReservationStatus.RESERVED
              and p.order.status =
                  com.equisibe.model.OrderStatus.PENDING_PAYMENT
              and p.createdAt < :cutoff
            order by coalesce(p.lastReconciledAt, p.createdAt), p.id
            """)
    java.util.List<ReconciliationCandidate> findReconciliationCandidates(
            @org.springframework.data.repository.query.Param("cutoff")
            java.time.Instant cutoff,
            org.springframework.data.domain.Pageable pageable);

    interface ReconciliationCandidate {
        Long getPaymentId();
        Long getOrderId();
        String getEmail();
        String getSessionId();
    }
}