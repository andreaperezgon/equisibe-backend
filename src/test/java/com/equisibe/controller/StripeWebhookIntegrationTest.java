package com.equisibe.controller;

import com.equisibe.service.OrderPaymentService;
import com.stripe.Stripe;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "STRIPE_WEBHOOK_SECRET=whsec_test_webhook_secret"
})
@ActiveProfiles("test")
class StripeWebhookIntegrationTest {

    private static final String PATH = "/api/payments/stripe/webhook";
    private static final String SECRET = "whsec_test_webhook_secret";

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private OrderPaymentService orderPaymentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void acceptsSignedPaidSessionWithoutLoginOrCsrf() throws Exception {
        String body = event("checkout.session.completed", "paid", false);

        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", sign(body))
                        .content(body))
                .andExpect(status().isNoContent());

        verify(orderPaymentService).confirmPayment(
                42L, "cs_test_example", 4395L, "eur"
        );
    }

    @Test
    void acceptsSignedAsynchronousPayment() throws Exception {
        String body = event(
                "checkout.session.async_payment_succeeded", "paid", false
        );

        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", sign(body))
                        .content(body))
                .andExpect(status().isNoContent());

        verify(orderPaymentService).confirmPayment(
                42L, "cs_test_example", 4395L, "eur"
        );
    }

    @Test
    void rejectsMissingSignature() throws Exception {
        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(event(
                                "checkout.session.completed", "paid", false
                        )))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderPaymentService);
    }

    @Test
    void rejectsTamperedPayload() throws Exception {
        String original = event(
                "checkout.session.completed", "paid", false
        );
        String tampered = original.replace("4395", "1");

        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", sign(original))
                        .content(tampered))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderPaymentService);
    }

    @Test
    void ignoresUnpaidSession() throws Exception {
        String body = event(
                "checkout.session.completed", "unpaid", false
        );

        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", sign(body))
                        .content(body))
                .andExpect(status().isNoContent());

        verifyNoInteractions(orderPaymentService);
    }

    @Test
    void rejectsLiveEvent() throws Exception {
        String body = event("checkout.session.completed", "paid", true);

        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", sign(body))
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderPaymentService);
    }

    @Test
    void ignoresUnrelatedEvent() throws Exception {
        String body = event("checkout.session.expired", "unpaid", false);

        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", sign(body))
                        .content(body))
                .andExpect(status().isNoContent());

        verifyNoInteractions(orderPaymentService);
    }

    @Test
    void keepsCsrfProtectionForOrderCreation() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .with(user("cliente@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(orderPaymentService);
    }

    private String event(
            String type,
            String paymentStatus,
            boolean live) {

        return """
                {
                  "id": "evt_test_example",
                  "object": "event",
                  "api_version": "%s",
                  "type": "%s",
                  "livemode": %s,
                  "data": {
                    "object": {
                      "id": "cs_test_example",
                      "object": "checkout.session",
                      "mode": "payment",
                      "payment_status": "%s",
                      "client_reference_id": "42",
                      "amount_total": 4395,
                      "currency": "eur",
                      "livemode": %s
                    }
                  }
                }
                """.formatted(
                Stripe.API_VERSION,
                type,
                live,
                paymentStatus,
                live
        );
    }

    private String sign(String body) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        String signedPayload = timestamp + "." + body;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                SECRET.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        ));

        String signature = HexFormat.of().formatHex(
                mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8))
        );

        return "t=" + timestamp + ",v1=" + signature;
    }
}