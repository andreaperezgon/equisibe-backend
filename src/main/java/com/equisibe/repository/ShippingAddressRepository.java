package com.equisibe.repository;

import com.equisibe.model.ShippingAddress;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ShippingAddressRepository
        extends JpaRepository<ShippingAddress, Long> {

    List<ShippingAddress> findByUser_IdOrderByIdDesc(Long userId);

    Optional<ShippingAddress> findByIdAndUser_Id(
            Long id,
            Long userId
    );
}