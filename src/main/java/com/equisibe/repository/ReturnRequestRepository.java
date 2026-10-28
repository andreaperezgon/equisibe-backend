package com.equisibe.repository;

import com.equisibe.model.ReturnRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReturnRequestRepository
        extends JpaRepository<ReturnRequest, Long> {

    Optional<ReturnRequest> findByClientRequestId(UUID clientRequestId);

    List<ReturnRequest> findByOrder_IdOrderByCreatedAtDescIdDesc(Long orderId);

    @EntityGraph(attributePaths = {"order", "order.user", "items"})
    List<ReturnRequest> findAllByOrderByCreatedAtDescIdDesc();
}
