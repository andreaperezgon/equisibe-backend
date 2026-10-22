package com.equisibe.service;

import com.equisibe.repository.OrderPaymentRepository;
import com.stripe.StripeClient;
import com.stripe.net.RequestOptions;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class StockReconciliationService {

    private static final Logger LOG =
            LoggerFactory.getLogger(StockReconciliationService.class);

    private final OrderPaymentRepository payments;
    private final OrderPaymentService paymentService;
    private final StripeCheckoutService checkoutService;
    private final ObjectProvider<StripeClient> stripeProvider;

    public StockReconciliationService(
            OrderPaymentRepository payments,
            OrderPaymentService paymentService,
            StripeCheckoutService checkoutService,
            ObjectProvider<StripeClient> stripeProvider) {
        this.payments = payments;
        this.paymentService = paymentService;
        this.checkoutService = checkoutService;
        this.stripeProvider = stripeProvider;
    }

    public void reconcilePending() {
        var candidates = payments.findReconciliationCandidates(
                Instant.now().minusSeconds(60),
                PageRequest.of(0, 25));

        if (candidates.isEmpty()) return;

        StripeClient stripe;
        try {
            stripe = stripeProvider.getObject();
        } catch (RuntimeException failure) {
            LOG.warn("No se pudo iniciar la comprobación de reservas con Stripe.");
            return;
        }

        var options = RequestOptions.builder()
                .setConnectTimeout(3000)
                .setReadTimeout(8000)
                .setMaxNetworkRetries(0)
                .build();

        for (var candidate : candidates) {
            try {
                if (candidate.getSessionId() == null) {
                    // Recupera con la misma clave de idempotencia.
                    // Nunca libera stock por desconocer el resultado del pago.
                    checkoutService.createSession(
                            candidate.getEmail(), candidate.getOrderId());
                    continue;
                }

                var session = stripe.v1().checkout().sessions()
                        .retrieve(candidate.getSessionId(), options);

                if (!candidate.getSessionId().equals(session.getId())
                        || Boolean.TRUE.equals(session.getLivemode())
                        || !"payment".equals(session.getMode())
                        || !String.valueOf(candidate.getOrderId())
                                .equals(session.getClientReferenceId())) {
                    LOG.warn("La sesión del pedido {} requiere revisión.",
                            candidate.getOrderId());
                    continue;
                }

                if ("complete".equals(session.getStatus())
                        && "paid".equals(session.getPaymentStatus())) {
                    paymentService.confirmPayment(
                            candidate.getOrderId(),
                            session.getId(),
                            session.getAmountTotal(),
                            session.getCurrency());
                } else if ("expired".equals(session.getStatus())
                        && session.getExpiresAt() != null) {
                    paymentService.expirePayment(
                            candidate.getOrderId(),
                            session.getId(),
                            Instant.ofEpochSecond(session.getExpiresAt()));
                }
            } catch (Exception failure) {
                LOG.warn(
                        "No se pudo reconciliar el pedido {}. Se conserva su reserva para revisión o reintento.",
                        candidate.getOrderId());
            } finally {
                try {
                    paymentService.markStockReconciled(candidate.getPaymentId());
                } catch (RuntimeException failure) {
                    LOG.warn("No se pudo registrar la comprobación del pedido {}.",
                            candidate.getOrderId());
                }
            }
        }
    }
}
