package com.equisibe.controller;

import com.equisibe.model.Role;
import com.equisibe.model.ShopSettings;
import com.equisibe.repository.ShopSettingsRepository;
import com.equisibe.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ShopSettingsController {
    private final ShopSettingsRepository settings;
    private final UserRepository users;

    public ShopSettingsController(
            ShopSettingsRepository settings, UserRepository users) {
        this.settings = settings;
        this.users = users;
    }

    @GetMapping("/api/shop/status")
    @Transactional(readOnly = true)
    public ResponseEntity<ShopStatus> status() {
        var current = settings.findById(1L).orElseGet(ShopSettings::new);
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(ShopStatus.from(current));
    }

    @PutMapping("/api/admin/shop/settings")
    @Transactional
    public ShopStatus update(
            @Valid @RequestBody SettingsRequest request,
            Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        users.findByEmail(principal.getName())
                .filter(user -> user.isEnabled() && user.getRole() == Role.ADMIN)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.FORBIDDEN));

        if (request.countdownEnabled() && request.countdownTarget() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Indica la fecha de la cuenta atrás."
            );
        }

        var current = settings.findById(1L).orElseGet(ShopSettings::new);
        current.update(
                request.closed(), request.title(), request.text(),
                request.countdownEnabled(), request.countdownTarget()
        );
        return ShopStatus.from(settings.saveAndFlush(current));
    }

    public record SettingsRequest(
            @NotNull Boolean closed,
            @NotBlank @Size(max = 120) String title,
            @NotBlank @Size(max = 2000) String text,
            @NotNull Boolean countdownEnabled,
            Instant countdownTarget
    ) {
    }

    public record ShopStatus(
            boolean closed,
            String title,
            String text,
            boolean countdownEnabled,
            Instant countdownTarget,
            Instant serverNow
    ) {
        public static ShopStatus from(ShopSettings settings) {
            return new ShopStatus(
                    settings.isClosed(), settings.getTitle(),
                    settings.getText(), settings.isCountdownEnabled(),
                    settings.getCountdownTarget(), Instant.now()
            );
        }
    }
}
