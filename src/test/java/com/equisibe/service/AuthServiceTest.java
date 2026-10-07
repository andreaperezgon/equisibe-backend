package com.equisibe.service;

import com.equisibe.dto.RegisterRequest;
import com.equisibe.exception.EmailAlreadyExistsException;
import com.equisibe.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.equisibe.model.Role;
import com.equisibe.model.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void shouldRejectExistingEmail() {
        var request = new RegisterRequest(
                "Andrea", "andrea@example.com", "ExamplePassword123"
        );
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.register(request)
        );

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }
    @Test
void shouldRegisterCustomerWithEncodedPassword() {
    var request = new RegisterRequest(
            " Andrea ", "ANDREA@example.com", "ExamplePassword123"
    );
    when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
    when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    var response = authService.register(request);

    verify(userRepository).existsByEmail("andrea@example.com");
    verify(passwordEncoder).encode(request.password());
    verify(userRepository).save(argThat(user ->
            user.getName().equals("Andrea")
            && user.getEmail().equals("andrea@example.com")
            && user.getPassword().equals("encoded-password")
            && user.getRole() == Role.CUSTOMER
            && user.isEnabled()
    ));

    assertEquals("Andrea", response.name());
    assertEquals("andrea@example.com", response.email());
    assertEquals(Role.CUSTOMER, response.role());
}
}