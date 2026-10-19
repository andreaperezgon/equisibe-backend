package com.equisibe.controller;

import com.equisibe.dto.PopupRequest;
import com.equisibe.dto.PopupResponse;
import com.equisibe.service.PopupService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PopupController {
    private final PopupService service;

    public PopupController(PopupService service) {
        this.service = service;
    }

    @GetMapping("/api/popups/active")
    public ResponseEntity<PopupResponse> active() {
        return service.getActive()
                .map(popup -> ResponseEntity.ok()
                        .header("Cache-Control", "no-store")
                        .body(popup))
                .orElseGet(() -> ResponseEntity.noContent()
                        .header("Cache-Control", "no-store").build());
    }

    @GetMapping("/api/admin/popups")
    public List<PopupResponse> list(Principal principal) {
        return service.getAll(email(principal));
    }

    @PostMapping("/api/admin/popups")
    public ResponseEntity<PopupResponse> create(
            @Valid @RequestBody PopupRequest request,
            Principal principal) {
        var popup = service.create(email(principal), request);
        return ResponseEntity.created(
                URI.create("/api/admin/popups/" + popup.id())
        ).body(popup);
    }

    @PutMapping("/api/admin/popups/{id}")
    public PopupResponse update(
            @PathVariable Long id,
            @Valid @RequestBody PopupRequest request,
            Principal principal) {
        return service.update(email(principal), id, request);
    }

    @PutMapping("/api/admin/popups/{id}/active")
    public PopupResponse activate(
            @PathVariable Long id,
            @Valid @RequestBody ActivationRequest request,
            Principal principal) {
        return service.setActive(email(principal), id, request.active());
    }

    @DeleteMapping("/api/admin/popups/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id, Principal principal) {
        service.delete(email(principal), id);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/api/admin/popups/examples")
    public List<PopupResponse> examples(Principal principal) {
        return service.createExamples(email(principal));
    }

    private String email(Principal principal) {
        return principal == null ? null : principal.getName();
    }

    public record ActivationRequest(@NotNull Boolean active) {
    }
}
