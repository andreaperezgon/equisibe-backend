package com.equisibe.repository;

import com.equisibe.model.PromotionalPopup;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionalPopupRepository
        extends JpaRepository<PromotionalPopup, Long> {

    boolean existsByPromoCode(String promoCode);

    boolean existsByPromoCodeAndActiveTrue(String promoCode);

    List<PromotionalPopup> findAllByOrderByUpdatedAtDescIdDesc();

    Optional<PromotionalPopup> findFirstByActiveTrueOrderByUpdatedAtDescIdDesc();
}
