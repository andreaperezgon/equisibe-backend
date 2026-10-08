package com.equisibe.controller;

import com.equisibe.dto.RegisterRequest;
import com.equisibe.dto.UserResponse;
import com.equisibe.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.equisibe.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;

@CrossOrigin(
        origins = "http://localhost:5173",
        allowCredentials = "true"
)@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(
            AuthService authService,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        UserResponse user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
  @PostMapping("/login")
public ResponseEntity<Void> login(
        @Valid @RequestBody LoginRequest credentials,
        HttpServletRequest request,
        HttpServletResponse response) {

    var token = UsernamePasswordAuthenticationToken.unauthenticated(
            credentials.email().trim().toLowerCase(Locale.ROOT),
            credentials.password()
    );

    var authentication = authenticationManager.authenticate(token);

    if (request.getSession(false) != null) {
        request.changeSessionId();
    }

    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    securityContextRepository.saveContext(context, request, response);

    return ResponseEntity.noContent().build();
}  
@GetMapping("/me")
public UserResponse me(Principal principal) {
    return authService.getCurrentUser(principal.getName());
}
}