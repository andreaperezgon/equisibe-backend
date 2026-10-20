package com.equisibe.controller;

import com.equisibe.dto.ReturnRequestInput;
import com.equisibe.dto.ReturnResponse;
import com.equisibe.service.ReturnRequestService;
import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders/{orderId}/returns")
public class ReturnRequestController {
    private final ReturnRequestService service;

    public ReturnRequestController(ReturnRequestService service) {
        this.service = service;
    }

    @GetMapping
    public List<ReturnResponse> list(
            @PathVariable Long orderId, Principal principal) {
        return service.list(email(principal), orderId);
    }

    @PostMapping
    public ResponseEntity<ReturnResponse> create(
            @PathVariable Long orderId,
            @Valid @RequestBody ReturnRequestInput input,
            Principal principal) {
        var result = service.create(email(principal), orderId, input);
        return ResponseEntity.created(
                URI.create("/api/orders/" + orderId + "/returns")
        ).body(result);
    }

    private String email(Principal principal) {
        return principal == null ? null : principal.getName();
    }
}
