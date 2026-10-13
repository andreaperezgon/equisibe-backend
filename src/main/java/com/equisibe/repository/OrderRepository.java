package com.equisibe.repository;

import com.equisibe.model.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUser_IdOrderByCreatedAtDescIdDesc(Long userId);

    Optional<Order> findByIdAndUser_Id(Long id, Long userId);
}