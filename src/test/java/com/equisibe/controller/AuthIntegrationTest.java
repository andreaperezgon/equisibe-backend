package com.equisibe.controller;

import com.equisibe.model.User;
import com.equisibe.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    @Test
    void shouldLoginMaintainSessionAndLogout() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        String email = "session-test@example.com";

        userRepository.saveAndFlush(new User(
                "Session Test",
                email,
                passwordEncoder.encode("ExamplePassword123")
        ));

        try {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().is4xxClientError());

            var login = mockMvc.perform(post("/api/auth/login")
                    .contentType("application/json")
                    .content("""
                            {
                              "email": "session-test@example.com",
                              "password": "ExamplePassword123"
                            }
                            """))
                    .andExpect(status().isNoContent())
                    .andReturn();

            var session = (MockHttpSession) login.getRequest()
                    .getSession(false);

            assertNotNull(session);

            mockMvc.perform(get("/api/auth/me").session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(email))
                    .andExpect(jsonPath("$.role").value("CUSTOMER"))
                    .andExpect(jsonPath("$.password").doesNotExist());

            mockMvc.perform(post("/api/auth/logout").session(session))
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