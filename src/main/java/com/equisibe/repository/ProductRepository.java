package com.equisibe.repository;

import com.equisibe.model.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrueOrderByIdDesc();

    Optional<Product> findByIdAndActiveTrue(Long id);

    @org.springframework.data.jpa.repository.Lock(
            jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query(
            "select p from Product p where p.id = :id")
    Optional<Product> findLockedById(
            @org.springframework.data.repository.query.Param("id") Long id);
}