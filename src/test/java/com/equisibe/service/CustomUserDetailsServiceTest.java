package com.equisibe.service;

import com.equisibe.model.User;
import com.equisibe.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void shouldLoadUserByNormalizedEmail() {
        var user = new User("Andrea", "andrea@example.com", "password-hash");

        when(userRepository.findByEmail("andrea@example.com"))
                .thenReturn(Optional.of(user));

        var result = service.loadUserByUsername(" ANDREA@example.com ");

        assertEquals("andrea@example.com", result.getUsername());
        assertEquals("password-hash", result.getPassword());
        assertTrue(result.isEnabled());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_CUSTOMER")));
    }
    @Test
void shouldRejectUnknownEmail() {
    when(userRepository.findByEmail("unknown@example.com"))
            .thenReturn(Optional.empty());

    assertThrows(
            UsernameNotFoundException.class,
            () -> service.loadUserByUsername("unknown@example.com")
    );
}
}