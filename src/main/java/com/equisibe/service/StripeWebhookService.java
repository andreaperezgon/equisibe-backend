package com.equisibe.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StripeWebhookService {

    private final OrderPaymentService orderPaymentService;
    private final String webhookSecret;

    public StripeWebhookService(
            OrderPaymentService orderPaymentService,
            @Value("${STRIPE_WEBHOOK_SECRET:}") String webhookSecret) {

        this.orderPaymentService = orderPaymentService;
        this.webhookSecret = webhookSecret.trim();
    }

    public void handle(String payload, String signature) {
        Event event = verifyEvent(payload, signature);

        if (Boolean.TRUE.equals(event.getLivemode())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Solo se admiten eventos de prueba."
            );
        }

        if (!"checkout.session.expired".equals(event.getType())
                && !"checkout.session.completed".equals(event.getType())
                && !"checkout.session.async_payment_succeeded"
                        .equals(event.getType())) {
            return;
        }

        var object = event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "No se ha podido interpretar el evento de Stripe."
                ));

        if (!(object instanceof Session session)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El evento no contiene una sesión de Checkout."
            );
        }


        if ("checkout.session.expired".equals(event.getType())) {
            if ("payment".equals(session.getMode())
                    && "expired".equals(session.getStatus())
                    && session.getExpiresAt() != null) {
                orderPaymentService.expirePayment(
                        getOrderId(session),
                        session.getId(),
                        java.time.Instant.ofEpochSecond(session.getExpiresAt()));
            }
            return;
        }

        if (!"payment".equals(session.getMode())
                || !"paid".equals(session.getPaymentStatus())) {
            return;
        }

        orderPaymentService.confirmPayment(
                getOrderId(session),
                session.getId(),
                session.getAmountTotal(),
                session.getCurrency()
        );
    }

    private Event verifyEvent(String payload, String signature) {
        if (!webhookSecret.startsWith("whsec_")) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "El webhook de Stripe no está configurado."
            );
        }

        if (payload == null || signature == null || signature.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Falta el contenido o la firma de Stripe."
            );
        }

        try {
            return Webhook.constructEvent(
                    payload,
                    signature,
                    webhookSecret
            );
        } catch (SignatureVerificationException exception) {
    throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "La firma de Stripe no es válida."
    );
} catch (RuntimeException exception) {
    throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "El contenido del evento de Stripe no es válido."
    );
}
    }

    private Long getOrderId(Session session) {
        String reference = session.getClientReferenceId();

        try {
            long orderId = Long.parseLong(reference);

            if (orderId <= 0) {
                throw new NumberFormatException();
            }

            return orderId;
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La sesión no contiene un pedido válido."
            );
        }
    }
}