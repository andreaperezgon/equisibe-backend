package com.equisibe.service;

import com.stripe.Stripe;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebhookFailureScenariosTest {
    static final String SECRET = "whsec_unit_test_only";
    final OrderPaymentService payments = mock(OrderPaymentService.class);
    final StripeWebhookService service = new StripeWebhookService(payments, SECRET);

    String event(String type, String object) {
        return """
                {"id":"evt_unit","object":"event","api_version":"%s",
                 "type":"%s","livemode":false,"data":{"object":%s}}
                """.formatted(Stripe.API_VERSION, type, object);
    }
    String session(String mode, String status, String paymentStatus, String reference, String expires) {
        return """
                {"id":"cs_test_unit","object":"checkout.session","mode":"%s",
                 "status":"%s","payment_status":"%s","client_reference_id":%s,
                 "expires_at":%s,"amount_total":4395,"currency":"eur","livemode":false}
                """.formatted(mode, status, paymentStatus, reference, expires);
    }
    String sign(String body) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(
                mac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8)));
    }
    void handle(String body) throws Exception { service.handle(body, sign(body)); }
    void error(int status, org.junit.jupiter.api.function.Executable action) {
        assertEquals(status, assertThrows(ResponseStatusException.class, action).getStatusCode().value());
    }
    @ParameterizedTest @ValueSource(strings={"", " ", "invalid"})
    void unavailableWithoutSigningSecret(String secret) {
        error(503, () -> new StripeWebhookService(payments, secret).handle("{}", "anything"));
        verifyNoInteractions(payments);
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={" "})
    void rejectsMissingSignature(String signature) {
        error(400, () -> service.handle("{}", signature));
        verifyNoInteractions(payments);
    }
    @Test void rejectsMissingPayloadInvalidSignatureAndSignedMalformedJson() throws Exception {
        error(400, () -> service.handle(null, "signature"));
        error(400, () -> service.handle("{}", "invalid"));
        String malformed = "not JSON";
        error(400, () -> service.handle(malformed, sign(malformed)));
        verifyNoInteractions(payments);
    }
    @Test void ignoresUnrelatedEventsWithoutAttemptingToParseCheckout() throws Exception {
        handle(event("customer.created", "{\"object\":\"customer\",\"id\":\"cus_unit\"}"));
        verifyNoInteractions(payments);
    }
    @Test void requiresCheckoutObjectAndCompatibleApiVersion() throws Exception {
        String wrongObject = event("checkout.session.completed", "{\"object\":\"customer\",\"id\":\"cus_unit\"}");
        error(400, () -> handle(wrongObject));
        String incompatible = event("checkout.session.completed",
                session("payment", "complete", "paid", "\"1\"", "1799999999"))
                .replace(Stripe.API_VERSION, "2000-01-01");
        error(500, () -> handle(incompatible));
        verifyNoInteractions(payments);
    }
    @Test void rejectsProductionEvent() throws Exception {
        String body = event("checkout.session.completed",
                session("payment", "complete", "paid", "\"1\"", "1799999999"))
                .replace("\"livemode\":false", "\"livemode\":true");
        error(400, () -> handle(body));
        verifyNoInteractions(payments);
    }
    @ParameterizedTest @ValueSource(strings={"null", "\"\"", "\"abc\"", "\"0\"", "\"-1\"", "\"999999999999999999999999\""})
    void rejectsInvalidOrderReference(String reference) {
        String body = event("checkout.session.completed",
                session("payment", "complete", "paid", reference, "1799999999"));
        error(400, () -> handle(body));
        verifyNoInteractions(payments);
    }
    @Test void ignoresUnpaidAndNonPaymentSessions() throws Exception {
        handle(event("checkout.session.completed", session("payment", "complete", "unpaid", "\"1\"", "1799999999")));
        handle(event("checkout.session.completed", session("subscription", "complete", "paid", "\"1\"", "1799999999")));
        verifyNoInteractions(payments);
    }
    @Test void expiryReleasesMatchingPayment() throws Exception {
        handle(event("checkout.session.expired", session("payment", "expired", "unpaid", "\"1\"", "1799999999")));
        verify(payments).expirePayment(1L, "cs_test_unit", Instant.ofEpochSecond(1799999999L));
        verifyNoMoreInteractions(payments);
    }
    @Test void ignoresExpiryWhenModeStateOrExpiryTimestampDoesNotMatch() throws Exception {
        handle(event("checkout.session.expired", session("subscription", "expired", "unpaid", "\"1\"", "1799999999")));
        handle(event("checkout.session.expired", session("payment", "open", "unpaid", "\"1\"", "1799999999")));
        handle(event("checkout.session.expired", session("payment", "expired", "unpaid", "\"1\"", "null")));
        verifyNoInteractions(payments);
    }
    @Test void confirmsSignedSynchronousAndAsynchronousPayments() throws Exception {
        String checkout = session("payment", "complete", "paid", "\"1\"", "1799999999");
        handle(event("checkout.session.completed", checkout));
        handle(event("checkout.session.async_payment_succeeded", checkout));
        verify(payments, times(2)).confirmPayment(1L, "cs_test_unit", 4395L, "eur");
        verifyNoMoreInteractions(payments);
    }
}
