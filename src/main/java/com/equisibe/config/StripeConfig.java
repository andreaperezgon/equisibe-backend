package com.equisibe.config;

import com.stripe.StripeClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class StripeConfig {

    @Bean
    @Lazy
    public StripeClient stripeClient(
            @Value("${STRIPE_SECRET_KEY:}") String secretKey) {

        String key = secretKey.trim();

        if (!key.startsWith("sk_test_")) {
            throw new IllegalStateException(
                    "Configura STRIPE_SECRET_KEY con una clave "
                            + "secreta de prueba de Stripe."
            );
        }

        return new StripeClient(key);
    }
}