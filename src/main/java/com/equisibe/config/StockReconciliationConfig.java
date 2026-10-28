package com.equisibe.config;

import com.equisibe.service.StockReconciliationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@Profile("!test")
public class StockReconciliationConfig {

    private final StockReconciliationService service;
    private final boolean enabled;

    public StockReconciliationConfig(
            StockReconciliationService service,
            @Value("${app.stock.reconciliation.enabled:true}") boolean enabled) {
        this.service = service;
        this.enabled = enabled;
    }

    @Scheduled(initialDelay = 60000, fixedDelay = 60000)
    public void checkReservations() {
        if (enabled) service.reconcilePending();
    }
}
