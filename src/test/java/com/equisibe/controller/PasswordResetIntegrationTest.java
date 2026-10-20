package com.equisibe.controller;

import com.equisibe.model.PasswordResetToken;
import com.equisibe.model.User;
import com.equisibe.repository.PasswordResetTokenRepository;
import com.equisibe.repository.UserRepository;
import com.equisibe.service.EmailService;
import com.equisibe.service.PasswordResetService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PasswordResetIntegrationTest {

    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired PasswordResetTokenRepository tokens;
    @Autowired PasswordResetService service;
    @Autowired PasswordEncoder encoder;
    @MockitoBean EmailService emailService;

    private MockMvc mvc;
    private User user;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity()).build();

        user = users.saveAndFlush(new User(
                "Prueba",
                UUID.randomUUID() + "@example.com",
                encoder.encode("PreviousPassword123")));
    }

    private String requestToken() {
        service.requestReset(user.getEmail());

        var link = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordReset(eq(user.getEmail()), link.capture());

        String url = link.getValue();
        return java.net.URI.create(url).getRawQuery().split("token=")[1].split("&")[0];
    }

    private String hash(String token) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void shouldStoreOnlyHashAndAllowOneUse() throws Exception {
        String token = requestToken();

        assertTrue(tokens.findByTokenHash(hash(token)).isPresent());
        assertTrue(tokens.findByTokenHash(token).isEmpty());

        service.resetPassword(token, "NewPassword123");

        assertTrue(encoder.matches("NewPassword123", user.getPassword()));
        assertFalse(encoder.matches("PreviousPassword123", user.getPassword()));
        assertEquals(1, user.getCredentialVersion());

        assertThrows(ResponseStatusException.class,
                () -> service.resetPassword(token, "AnotherPassword123"));
    }

    @Test
    void shouldRejectExpiredLink() throws Exception {
        String token = "a".repeat(43);
        tokens.saveAndFlush(new PasswordResetToken(
                hash(token), user, Instant.now().minusSeconds(1)));

        var error = assertThrows(ResponseStatusException.class,
                () -> service.resetPassword(token, "NewPassword123"));

        assertEquals(400, error.getStatusCode().value());
        assertTrue(encoder.matches("PreviousPassword123", user.getPassword()));
    }

    @Test
    void shouldGiveGenericResponseForUnknownEmail() throws Exception {
        mvc.perform(post("/api/auth/forgot-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + UUID.randomUUID() + "@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "Si el correo está registrado, recibirás un enlace para cambiar tu contraseña."));

        verify(emailService, never()).sendPasswordReset(anyString(), anyString());
    }

    @Test
    void shouldLimitRepeatedEmails() {
        service.requestReset(user.getEmail());
        service.requestReset(user.getEmail());

        verify(emailService, times(1))
                .sendPasswordReset(eq(user.getEmail()), anyString());
    }

    @Test
    void shouldRequireCsrfAndValidatePassword() throws Exception {
        mvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/auth/reset-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + "a".repeat(43) + "\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldInvalidateOldSessionAndAcceptNewPassword() throws Exception {
        var login = mvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + user.getEmail()
                        + "\",\"password\":\"PreviousPassword123\"}"))
                .andExpect(status().isNoContent())
                .andReturn();

        var session = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(session);

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk());

        service.resetPassword(requestToken(), "NewPassword123");

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + user.getEmail()
                        + "\",\"password\":\"NewPassword123\"}"))
                .andExpect(status().isNoContent());
    }
}
