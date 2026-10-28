package com.equisibe.controller;

import com.equisibe.service.PasswordResetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

    private final PasswordResetService service;

    public PasswordResetController(PasswordResetService service) {
        this.service = service;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Message> request(@Valid @RequestBody ForgotInput input) {
        service.requestReset(input.email());
        return ResponseEntity.ok(new Message(
                "Si el correo está registrado, recibirás un enlace para cambiar tu contraseña."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> reset(@Valid @RequestBody ResetInput input) {
        service.resetPassword(input.token(), input.password());
        return ResponseEntity.noContent().build();
    }

    public record ForgotInput(
            @NotBlank @Email @Size(max = 254) String email) {
    }

    public record ResetInput(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token,
            @NotBlank @Size(min = 8, max = 64) String password) {
    }

    public record Message(String message) {
    }
}
