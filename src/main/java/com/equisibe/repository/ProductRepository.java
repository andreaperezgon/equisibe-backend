package com.equisibe.repository;

import com.equisibe.model.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrueOrderByIdDesc();

    Optional<Product> findByIdAndActiveTrue(Long id);
}