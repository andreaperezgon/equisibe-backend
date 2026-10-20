package com.equisibe.controller;

import com.equisibe.model.ReturnStatus;
import com.equisibe.service.AdminReturnService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/returns")
public class AdminReturnController {
    private final AdminReturnService service;

    public AdminReturnController(AdminReturnService service) {
        this.service = service;
    }

    @GetMapping
    public List<AdminReturnService.AdminReturn> list(Principal principal) {
        return service.list(email(principal));
    }

    @PutMapping("/{id}/status")
    public AdminReturnService.AdminReturn update(
            @PathVariable Long id,
            @Valid @RequestBody StatusInput input,
            Principal principal) {
        return service.update(
                email(principal), id, input.status(), input.note()
        );
    }

    private String email(Principal principal) {
        return principal == null ? null : principal.getName();
    }

    public record StatusInput(
            @NotNull ReturnStatus status,
            @Size(max = 2000) String note
    ) {
    }
}
