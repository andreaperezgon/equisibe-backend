package com.equisibe.config;

import com.equisibe.service.StockReconciliationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentConfigurationTest {
    @ParameterizedTest @ValueSource(strings={"", " ", "sk_live_not_a_real_key", "invalid"})
    void rejectsMissingAndLiveStripeKeys(String key) {
        assertThrows(IllegalStateException.class, () -> new StripeConfig().stripeClient(key));
    }
    @Test void acceptsTrimmedTestKeyWithoutContactingStripe() {
        assertNotNull(new StripeConfig().stripeClient(" sk_test_not_a_real_key "));
    }
    @Test void disabledReconciliationDoesNotCheckPayments() {
        var service = mock(StockReconciliationService.class);
        new StockReconciliationConfig(service, false).checkReservations();
        verifyNoInteractions(service);
    }
    @Test void enabledReconciliationChecksPendingPayments() {
        var service = mock(StockReconciliationService.class);
        new StockReconciliationConfig(service, true).checkReservations();
        verify(service).reconcilePending();
    }
}
