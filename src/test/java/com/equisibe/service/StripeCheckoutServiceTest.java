package com.equisibe.service;

import com.equisibe.dto.OrderResponse;
import com.equisibe.model.ClothingSize;
import com.equisibe.model.OrderStatus;
import com.stripe.StripeClient;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeCheckoutServiceTest {

    private static final String EMAIL = "cliente@example.com";
    private static final String IDEMPOTENCY_KEY = "checkout-test-key";
    private static final long SESSION_EXPIRES_AT = 1791892800L;

    @Mock
    private OrderPaymentService orderPaymentService;

    @Mock
    private ObjectProvider<StripeClient> clientProvider;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private StripeClient client;

    private StripeCheckoutService service;

    @BeforeEach
    void setUp() {
        service = new StripeCheckoutService(
                orderPaymentService,
                clientProvider,
                "http://localhost:5173"
        );
    }

    @ParameterizedTest
    @CsvSource({
            "1, 3.95, 395",
            "2, 0.00, 0"
    })
    void sendsOrderAmountsToStripe(
            long quantity,
            String shipping,
            long shippingCents) throws Exception {

        when(orderPaymentService.prepare(EMAIL, 42L))
                .thenReturn(new OrderPaymentService.PreparedPayment(
                        order(OrderStatus.PENDING_PAYMENT, quantity, shipping),
                        IDEMPOTENCY_KEY,
                        null,
                        null,
                        Instant.parse("2026-10-13T10:00:00Z")
                ));

        when(clientProvider.getObject()).thenReturn(client);

        Session session = mock(Session.class);
        when(session.getStatus()).thenReturn("open");
        when(session.getId()).thenReturn("cs_test_example");
        when(session.getExpiresAt()).thenReturn(SESSION_EXPIRES_AT);
        when(session.getUrl())
                .thenReturn("https://checkout.stripe.com/example");

        var sessions = client.v1().checkout().sessions();

        when(sessions.create(
                any(SessionCreateParams.class),
                any(RequestOptions.class)
        )).thenReturn(session);

        var response = service.createSession(EMAIL, 42L);

        var paramsCaptor = ArgumentCaptor.forClass(
                SessionCreateParams.class
        );
        var optionsCaptor = ArgumentCaptor.forClass(
                RequestOptions.class
        );

        verify(sessions).create(
                paramsCaptor.capture(),
                optionsCaptor.capture()
        );

        var params = paramsCaptor.getValue();
        var line = params.getLineItems().getFirst();

        assertEquals(SessionCreateParams.Mode.PAYMENT, params.getMode());
        assertEquals("42", params.getClientReferenceId());
        assertEquals("42", params.getMetadata().get("orderId"));

        assertEquals(1, params.getLineItems().size());
        assertEquals(Long.valueOf(quantity), line.getQuantity());
        assertEquals(
                Long.valueOf(4000L),
                line.getPriceData().getUnitAmount()
        );
        assertEquals("eur", line.getPriceData().getCurrency());
        assertEquals(
                "Camiseta · Talla S",
                line.getPriceData().getProductData().getName()
        );

        var shippingAmount = params.getShippingOptions()
                .getFirst()
                .getShippingRateData()
                .getFixedAmount();

        assertEquals(
                Long.valueOf(shippingCents),
                shippingAmount.getAmount()
        );
        assertEquals("eur", shippingAmount.getCurrency());

        assertEquals(
                "http://localhost:5173/orders/42?payment=returned",
                params.getSuccessUrl()
        );
        assertEquals(
                "http://localhost:5173/orders/42?payment=cancelled",
                params.getCancelUrl()
        );
        assertEquals(
                IDEMPOTENCY_KEY,
                optionsCaptor.getValue().getIdempotencyKey()
        );

        assertEquals("cs_test_example", response.sessionId());
        assertEquals(
                "https://checkout.stripe.com/example",
                response.url()
        );

        verify(orderPaymentService).attachSession(
                EMAIL,
                42L,
                "cs_test_example",
                Instant.ofEpochSecond(SESSION_EXPIRES_AT)
        );
    }

    @Test
    void rejectsAlreadyPaidOrder() {
        when(orderPaymentService.prepare(EMAIL, 42L))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "El pedido no está pendiente de pago."
                ));

        var error = assertThrows(
                ResponseStatusException.class,
                () -> service.createSession(EMAIL, 42L)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verifyNoInteractions(clientProvider, client);
        verify(orderPaymentService, never()).attachSession(
                any(), any(), any(), any()
        );
    }

    @Test
    void doesNotContactStripeWhenOrderIsNotAccessible() {
        when(orderPaymentService.prepare(EMAIL, 42L))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El pedido no está disponible."
                ));

        var error = assertThrows(
                ResponseStatusException.class,
                () -> service.createSession(EMAIL, 42L)
        );

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(clientProvider, client);
        verify(orderPaymentService, never()).attachSession(
                any(), any(), any(), any()
        );
    }

    private OrderResponse order(
            OrderStatus status,
            long quantity,
            String shipping) {

        BigDecimal price = new BigDecimal("40.00");
        BigDecimal subtotal = price.multiply(
                BigDecimal.valueOf(quantity)
        );
        BigDecimal shippingCost = new BigDecimal(shipping);

        var address = new OrderResponse.Address(
                "Cliente de prueba",
                "600123456",
                "Calle Mayor 10",
                null,
                "Oviedo",
                "Asturias",
                "33001",
                "ES"
        );

        var item = new OrderResponse.Item(
                1L,
                "Camiseta",
                ClothingSize.S,
                quantity,
                price,
                subtotal
        );

        return new OrderResponse(
                42L,
                status,
                Instant.parse("2026-10-13T10:00:00Z"),
                address,
                List.of(item),
                subtotal,
                shippingCost,
                subtotal.add(shippingCost),
                "EUR"
        );
    }
}