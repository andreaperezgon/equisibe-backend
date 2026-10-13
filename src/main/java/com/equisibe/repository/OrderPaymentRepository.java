package com.equisibe.repository;

import com.equisibe.model.OrderPayment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderPaymentRepository
        extends JpaRepository<OrderPayment, Long> {

    Optional<OrderPayment> findByOrder_Id(Long orderId);

    Optional<OrderPayment> findByStripeSessionId(String stripeSessionId);
}