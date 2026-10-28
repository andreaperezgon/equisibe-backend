package com.equisibe.controller;

import com.equisibe.service.AdminReturnService;
import java.util.List;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class AdminReturnSecurityTest {

    @Autowired WebApplicationContext context;
    @MockitoBean AdminReturnService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity()).build();
    }

    @Test
    void shouldRejectAnonymousAccess() throws Exception {
        mvc.perform(get("/api/admin/returns"))
                .andExpect(status().is4xxClientError());

        verifyNoInteractions(service);
    }

    @Test
    void shouldRejectCustomerListingReturns() throws Exception {
        mvc.perform(get("/api/admin/returns")
                .with(user("cliente@example.com").roles("CUSTOMER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void shouldRejectCustomerChangingReturnStatus() throws Exception {
        mvc.perform(put("/api/admin/returns/7/status")
                .with(user("cliente@example.com").roles("CUSTOMER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "status": "IN_REVIEW",
                          "note": "Intento no autorizado"
                        }
                        """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void shouldRequireCsrfEvenForAdmin() throws Exception {
        mvc.perform(put("/api/admin/returns/7/status")
                .with(user("admin@example.com").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "status": "IN_REVIEW",
                          "note": "Revisión"
                        }
                        """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void shouldAllowAdminListingReturns() throws Exception {
        when(service.list("admin@example.com")).thenReturn(List.of());

        mvc.perform(get("/api/admin/returns")
                .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(service).list("admin@example.com");
    }
}
