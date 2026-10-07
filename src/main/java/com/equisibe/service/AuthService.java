package com.equisibe.service;

import com.equisibe.dto.RegisterRequest;
import com.equisibe.exception.EmailAlreadyExistsException;
import com.equisibe.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.equisibe.model.User;
import java.util.Locale;
import com.equisibe.dto.UserResponse;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

public UserResponse register(RegisterRequest request) {
            String name = request.name().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }
        String passwordHash = passwordEncoder.encode(request.password());
User user = new User(name, email, passwordHash);
User savedUser = userRepository.save(user);

return new UserResponse(
        savedUser.getId(),
        savedUser.getName(),
        savedUser.getEmail(),
        savedUser.getRole()
);    }
}