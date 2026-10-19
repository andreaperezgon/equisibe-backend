package com.equisibe.repository;

import com.equisibe.model.PromotionalCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionalCodeRepository
        extends JpaRepository<PromotionalCode, String> {
}
