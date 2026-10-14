package com.equisibe.controller;

import com.equisibe.service.StripeCheckoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class StripeCheckoutControllerTest {
    @Autowired private WebApplicationContext context;
    @MockitoBean private StripeCheckoutService checkoutService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void opensCheckoutForAuthenticatedCustomer() throws Exception {
        when(checkoutService.createSession("cliente@example.com", 12L))
                .thenReturn(new StripeCheckoutService.CheckoutSessionResponse(
                        "cs_test", "https://checkout.stripe.com/c/pay/cs_test"));
        mvc.perform(post("/api/orders/12/checkout-session")
                .with(user("cliente@example.com")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("cs_test"))
                .andExpect(jsonPath("$.url").value("https://checkout.stripe.com/c/pay/cs_test"));
        verify(checkoutService).createSession("cliente@example.com", 12L);
    }

    @Test
    void requiresLogin() throws Exception {
        mvc.perform(post("/api/orders/12/checkout-session").with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(checkoutService);
    }

    @Test
    void requiresCsrf() throws Exception {
        mvc.perform(post("/api/orders/12/checkout-session").with(user("cliente@example.com")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(checkoutService);
    }

    @Test
    void rejectsInvalidOrderId() throws Exception {
        mvc.perform(post("/api/orders/0/checkout-session")
                .with(user("cliente@example.com")).with(csrf()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(checkoutService);
    }
}
