package com.equisibe.service;

import com.equisibe.model.PasswordResetToken;
import com.equisibe.model.User;
import com.equisibe.repository.PasswordResetTokenRepository;
import com.equisibe.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class PasswordResetService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final EmailService emailService;
    private final EntityManager entityManager;
    private final String baseUrl;
    private final Map<String, Instant> attempts = new HashMap<>();

    public PasswordResetService(
            UserRepository users,
            PasswordResetTokenRepository tokens,
            PasswordEncoder encoder,
            EmailService emailService,
            EntityManager entityManager,
            @Value("${app.password-reset.base-url}") String baseUrl) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
        this.emailService = emailService;
        this.entityManager = entityManager;
        this.baseUrl = baseUrl;
    }

    @Transactional
    public void requestReset(String email) {
        emailService.requireAvailable();
        String normalized = email.trim().toLowerCase(Locale.ROOT);

        if (!allowRequest(hash(normalized))) {
            return;
        }

        var found = users.findByEmail(normalized);
        if (found.isEmpty()) {
            return;
        }

        User user = entityManager.find(
                User.class, found.get().getId(), LockModeType.PESSIMISTIC_WRITE);
        if (!user.isEnabled()) {
            return;
        }

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        tokens.deleteByUser_Id(user.getId());
        tokens.flush();
        tokens.saveAndFlush(new PasswordResetToken(
                hash(token), user, Instant.now().plus(30, ChronoUnit.MINUTES)));

        String link = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("token", token)
                .build().encode().toUriString();

        try {
            emailService.sendPasswordReset(user.getEmail(), link);
        } catch (ResponseStatusException exception) {
            // La respuesta pública no revela si existe una cuenta.
            LOG.warn("No se pudo entregar un correo de recuperación de contraseña.");
        }
    }

    @Transactional
    public void resetPassword(String token, String password) {
        var preview = tokens.findByTokenHash(hash(token))
                .orElseThrow(this::invalidLink);

        User user = entityManager.find(
                User.class, preview.getUser().getId(), LockModeType.PESSIMISTIC_WRITE);

        var stored = tokens.findLockedByHash(hash(token))
                .orElseThrow(this::invalidLink);

        if (!user.isEnabled() || !stored.isUsable(Instant.now())) {
            throw invalidLink();
        }

        user.changePassword(encoder.encode(password));
        stored.markUsed(Instant.now());
        tokens.deleteByUser_Id(user.getId());
        tokens.flush();
    }

    private synchronized boolean allowRequest(String key) {
        Instant now = Instant.now();
        attempts.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
        if (attempts.containsKey(key) || attempts.size() >= 5000) {
            return false;
        }
        attempts.put(key, now.plusSeconds(60));
        return true;
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private ResponseStatusException invalidLink() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El enlace ha caducado o ya se utilizó. Solicita uno nuevo.");
    }
}
