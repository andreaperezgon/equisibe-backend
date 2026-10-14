package com.equisibe.controller;

import com.equisibe.service.StripeWebhookService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StripeWebhookController {

    private final StripeWebhookService webhookService;

    public StripeWebhookController(StripeWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/api/payments/stripe/webhook")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void receive(
            @RequestBody String payload,
            @RequestHeader(
                    value = "Stripe-Signature",
                    required = false
            ) String signature) {

        webhookService.handle(payload, signature);
    }
}