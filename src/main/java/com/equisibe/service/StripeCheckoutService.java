package com.equisibe.service;

import com.equisibe.dto.OrderResponse;
import com.equisibe.model.OrderStatus;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import java.math.BigDecimal;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;

@Service
public class StripeCheckoutService {

    private final OrderPaymentService orderPaymentService;
    private final ObjectProvider<StripeClient> stripeClientProvider;
    private final String frontendUrl;

    public StripeCheckoutService(
            OrderPaymentService orderPaymentService,
            ObjectProvider<StripeClient> stripeClientProvider,
            @Value("${app.frontend-url:http://localhost:5173}")
            String frontendUrl) {

        this.orderPaymentService = orderPaymentService;
        this.stripeClientProvider = stripeClientProvider;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    public CheckoutSessionResponse createSession(
            String email,
            Long orderId) {

        var payment = orderPaymentService.prepare(email, orderId);
OrderResponse order = payment.order();

        if (order.status() != OrderStatus.PENDING_PAYMENT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El pedido no está pendiente de pago."
            );
        }

        var builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setLocale(SessionCreateParams.Locale.ES)
                .addAllowedPaymentMethodType(
                        SessionCreateParams.AllowedPaymentMethodType.CARD
                )
                .setClientReferenceId(order.id().toString())
                .putMetadata("orderId", order.id().toString())
                .setSuccessUrl(
                        frontendUrl + "/orders/" + order.id()
                                + "?payment=returned"
                )
                .setCancelUrl(
                        frontendUrl + "/orders/" + order.id()
                                + "?payment=cancelled"
                );

        for (OrderResponse.Item item : order.items()) {
            builder.addLineItem(createLineItem(item));
        }

        builder.addShippingOption(
                createShippingOption(order.shippingCost())
        );

        RequestOptions options = RequestOptions.builder()
        .setIdempotencyKey(payment.idempotencyKey())
        .build();

        try {
            var session = stripeClientProvider.getObject()
                    .v1()
                    .checkout()
                    .sessions()
                    .create(builder.build(), options);

            if (!"open".equals(session.getStatus())
                    || session.getUrl() == null) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "La sesión de pago ya no está disponible."
                );
            }
orderPaymentService.attachSession(
        email,
        orderId,
        session.getId(),
        Instant.ofEpochSecond(session.getExpiresAt())
);
            return new CheckoutSessionResponse(
                    session.getId(),
                    session.getUrl()
            );
        } catch (StripeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se ha podido abrir el pago. Inténtalo de nuevo."
            );
        }
    }

    private SessionCreateParams.LineItem createLineItem(
            OrderResponse.Item item) {

        var productData = SessionCreateParams.LineItem
                .PriceData.ProductData.builder()
                .setName(
                        item.productName() + " · Talla " + item.size()
                )
                .build();

        var priceData = SessionCreateParams.LineItem.PriceData.builder()
                .setCurrency("eur")
                .setUnitAmount(toCents(item.unitPrice()))
                .setProductData(productData)
                .build();

        return SessionCreateParams.LineItem.builder()
                .setQuantity(item.quantity())
                .setPriceData(priceData)
                .build();
    }

    private SessionCreateParams.ShippingOption createShippingOption(
            BigDecimal shippingCost) {

        var fixedAmount = SessionCreateParams.ShippingOption
                .ShippingRateData.FixedAmount.builder()
                .setCurrency("eur")
                .setAmount(toCents(shippingCost))
                .build();

        var shippingRate = SessionCreateParams.ShippingOption
                .ShippingRateData.builder()
                .setDisplayName("Envío a España")
                .setType(
                        SessionCreateParams.ShippingOption
                                .ShippingRateData.Type.FIXED_AMOUNT
                )
                .setFixedAmount(fixedAmount)
                .build();

        return SessionCreateParams.ShippingOption.builder()
                .setShippingRateData(shippingRate)
                .build();
    }

    private long toCents(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }

    public record CheckoutSessionResponse(
            String sessionId,
            String url
    ) {
    }
}