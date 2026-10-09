package com.equisibe.service;

import com.equisibe.dto.RegisterRequest;
import com.equisibe.exception.EmailAlreadyExistsException;
import com.equisibe.model.Role;
import com.equisibe.model.User;
import com.equisibe.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldNormalizeRegistrationDataAndEncodePassword() {
        var request = new RegisterRequest(
                " Andrea ",
                " Andrea@Example.com ",
                "Password123"
        );

        when(userRepository.existsByEmail("andrea@example.com"))
                .thenReturn(false);
        when(passwordEncoder.encode("Password123"))
                .thenReturn("encoded-password");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.register(request);

        var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        var savedUser = captor.getValue();

        assertEquals("Andrea", savedUser.getName());
        assertEquals("andrea@example.com", savedUser.getEmail());
        assertEquals("encoded-password", savedUser.getPassword());
        assertEquals(Role.CUSTOMER, savedUser.getRole());
        assertTrue(savedUser.isEnabled());

        assertEquals("Andrea", response.name());
        assertEquals("andrea@example.com", response.email());
        assertEquals(Role.CUSTOMER, response.role());
    }

    @Test
    void shouldRejectRegistrationWhenEmailAlreadyExists() {
        var request = new RegisterRequest(
                "Andrea",
                "Andrea@Example.com",
                "Password123"
        );

        when(userRepository.existsByEmail("andrea@example.com"))
                .thenReturn(true);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.register(request)
        );

        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldReturnCurrentUserProfile() {
        var user = new User(
                "Andrea",
                "andrea@example.com",
                "encoded-password"
        );

        when(userRepository.findByEmail("andrea@example.com"))
                .thenReturn(Optional.of(user));

        var response = authService.getCurrentUser("andrea@example.com");

        assertEquals("Andrea", response.name());
        assertEquals("andrea@example.com", response.email());
        assertEquals(Role.CUSTOMER, response.role());

        verify(userRepository).findByEmail("andrea@example.com");
    }

    @Test
    void shouldRejectSessionWhenUserDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                BadCredentialsException.class,
                () -> authService.getCurrentUser("missing@example.com")
        );
    }

    @Test
    void shouldRejectSessionWhenUserIsDisabled() {
        var user = mock(User.class);

        when(user.isEnabled()).thenReturn(false);
        when(userRepository.findByEmail("blocked@example.com"))
                .thenReturn(Optional.of(user));

        assertThrows(
                BadCredentialsException.class,
                () -> authService.getCurrentUser("blocked@example.com")
        );
    }
}