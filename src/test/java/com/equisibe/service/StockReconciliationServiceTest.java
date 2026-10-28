package com.equisibe.service;

import com.equisibe.repository.OrderPaymentRepository;
import com.stripe.StripeClient;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Pageable;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StockReconciliationServiceTest {

    private final OrderPaymentRepository payments =
            mock(OrderPaymentRepository.class);
    private final OrderPaymentService paymentService =
            mock(OrderPaymentService.class);
    private final StripeCheckoutService checkoutService =
            mock(StripeCheckoutService.class);

    @SuppressWarnings("unchecked")
    private final ObjectProvider<StripeClient> provider =
            mock(ObjectProvider.class);

    private final StripeClient client =
            mock(StripeClient.class, RETURNS_DEEP_STUBS);
    private final Session session = mock(Session.class);
    private final OrderPaymentRepository.ReconciliationCandidate candidate =
            mock(OrderPaymentRepository.ReconciliationCandidate.class);

    private final StockReconciliationService service =
            new StockReconciliationService(
                    payments, paymentService, checkoutService, provider);

    private void prepare() throws Exception {
        when(payments.findReconciliationCandidates(
                any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(candidate));

        when(candidate.getPaymentId()).thenReturn(9L);
        when(candidate.getOrderId()).thenReturn(42L);
        when(candidate.getEmail()).thenReturn("cliente@example.com");
        when(candidate.getSessionId()).thenReturn("cs_test_example");
        when(provider.getObject()).thenReturn(client);

        when(client.v1().checkout().sessions().retrieve(
                eq("cs_test_example"), any(RequestOptions.class)))
                .thenReturn(session);

        when(session.getId()).thenReturn("cs_test_example");
        when(session.getMode()).thenReturn("payment");
        when(session.getLivemode()).thenReturn(false);
        when(session.getClientReferenceId()).thenReturn("42");
        when(session.getStatus()).thenReturn("open");
    }

    @Test
    void confirmsPaidSessionWhenWebhookWasMissed() throws Exception {
        prepare();
        when(session.getStatus()).thenReturn("complete");
        when(session.getPaymentStatus()).thenReturn("paid");
        when(session.getAmountTotal()).thenReturn(4395L);
        when(session.getCurrency()).thenReturn("eur");

        service.reconcilePending();

        verify(paymentService).confirmPayment(
                42L, "cs_test_example", 4395L, "eur");
        verify(paymentService, never()).expirePayment(any(), any(), any());
        verify(paymentService).markStockReconciled(9L);
    }

    @Test
    void releasesOnlyAfterStripeConfirmsExpiration() throws Exception {
        prepare();
        when(session.getStatus()).thenReturn("expired");
        when(session.getExpiresAt()).thenReturn(1800000000L);

        service.reconcilePending();

        verify(paymentService).expirePayment(
                42L, "cs_test_example", Instant.ofEpochSecond(1800000000L));
        verify(paymentService, never()).confirmPayment(any(), any(), any(), any());
        verify(paymentService).markStockReconciled(9L);
    }

    @Test
    void preservesReservationOnConnectionFailure() throws Exception {
        prepare();
        when(client.v1().checkout().sessions().retrieve(
                eq("cs_test_example"), any(RequestOptions.class)))
                .thenThrow(new RuntimeException("Sin conexión"));

        service.reconcilePending();

        verify(paymentService, never()).expirePayment(any(), any(), any());
        verify(paymentService, never()).confirmPayment(any(), any(), any(), any());
        verify(paymentService).markStockReconciled(9L);
    }

    @Test
    void preservesReservationWhileSessionRemainsOpen() throws Exception {
        prepare();

        service.reconcilePending();

        verify(paymentService, never()).expirePayment(any(), any(), any());
        verify(paymentService, never()).confirmPayment(any(), any(), any(), any());
        verify(paymentService).markStockReconciled(9L);
    }

    @Test
    void rejectsSessionBelongingToAnotherOrder() throws Exception {
        prepare();
        when(session.getClientReferenceId()).thenReturn("999");
        when(session.getStatus()).thenReturn("expired");
        when(session.getExpiresAt()).thenReturn(1800000000L);

        service.reconcilePending();

        verify(paymentService, never()).expirePayment(any(), any(), any());
        verify(paymentService, never()).confirmPayment(any(), any(), any(), any());
    }

    @Test
    void recoversMissingSessionThroughExistingCheckoutFlow() throws Exception {
        prepare();
        when(candidate.getSessionId()).thenReturn(null);

        service.reconcilePending();

        verify(checkoutService).createSession("cliente@example.com", 42L);
        verify(paymentService, never()).expirePayment(any(), any(), any());
        verify(paymentService).markStockReconciled(9L);
    }

    @Test
    void doesNotContactStripeWhenThereAreNoReservations() {
        when(payments.findReconciliationCandidates(
                any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());

        service.reconcilePending();

        verifyNoInteractions(provider, paymentService, checkoutService);
    }
}
