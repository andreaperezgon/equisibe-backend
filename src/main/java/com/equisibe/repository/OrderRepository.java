package com.equisibe.repository;

import com.equisibe.model.Order;
import com.equisibe.model.OrderStatus;
import java.util.List;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = "items")
    List<Order> findByUser_IdOrderByCreatedAtDescIdDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "items"})
    List<Order> findByStatusInOrderByCreatedAtDescIdDesc(
            Collection<OrderStatus> statuses
    );

    Optional<Order> findByIdAndUser_Id(Long id, Long userId);
}
