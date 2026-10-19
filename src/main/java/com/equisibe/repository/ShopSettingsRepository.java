package com.equisibe.repository;

import com.equisibe.model.ShopSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopSettingsRepository
        extends JpaRepository<ShopSettings, Long> {
}
