package com.equisibe.controller;

import com.equisibe.model.User;
import com.equisibe.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void shouldRejectLoginWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "session-test@example.com",
                          "password": "ExamplePassword123"
                        }
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldLoginMaintainSessionAndLogoutWithCsrf() throws Exception {
        String email = "session-test@example.com";

        userRepository.saveAndFlush(new User(
                "Session Test",
                email,
                passwordEncoder.encode("ExamplePassword123")
        ));

        try {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().is4xxClientError());

            var csrfResult = mockMvc.perform(get("/api/auth/csrf"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.headerName")
                            .value("X-CSRF-TOKEN"))
                    .andReturn();

            var session = (MockHttpSession) csrfResult.getRequest()
                    .getSession(false);

            var loginToken = (CsrfToken) csrfResult.getRequest()
                    .getAttribute(CsrfToken.class.getName());

            assertNotNull(session);
            assertNotNull(loginToken);

            mockMvc.perform(post("/api/auth/login")
                    .session(session)
                    .header(loginToken.getHeaderName(), loginToken.getToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "email": "session-test@example.com",
                              "password": "ExamplePassword123"
                            }
                            """))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/auth/me").session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(email))
                    .andExpect(jsonPath("$.role").value("CUSTOMER"))
                    .andExpect(jsonPath("$.password").doesNotExist());

            mockMvc.perform(post("/api/auth/logout").session(session))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/auth/me").session(session))
                    .andExpect(status().isOk());

            var logoutCsrfResult = mockMvc.perform(
                    get("/api/auth/csrf").session(session))
                    .andExpect(status().isOk())
                    .andReturn();

            var logoutToken = (CsrfToken) logoutCsrfResult.getRequest()
                    .getAttribute(CsrfToken.class.getName());

            assertNotNull(logoutToken);

            mockMvc.perform(post("/api/auth/logout")
                    .session(session)
                    .header(logoutToken.getHeaderName(), logoutToken.getToken()))
                    .andExpect(status().isNoContent());

            assertTrue(session.isInvalid());

            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().is4xxClientError());
        } finally {
            userRepository.findByEmail(email)
                    .ifPresent(userRepository::delete);
        }
    }
}