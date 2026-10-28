package com.equisibe.service;

import com.equisibe.model.Role;
import com.equisibe.model.User;
import com.equisibe.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GoogleAccountServiceTest {
    UserRepository users;
    PasswordEncoder encoder;
    GoogleAccountService service;
    @BeforeEach void setup() {
        users = mock(UserRepository.class);
        encoder = mock(PasswordEncoder.class);
        service = new GoogleAccountService(users, encoder);
    }
    OidcUser identity(String subject, String email, Boolean verified) {
        var google = mock(OidcUser.class);
        when(google.getSubject()).thenReturn(subject);
        when(google.getEmail()).thenReturn(email);
        when(google.getEmailVerified()).thenReturn(verified);
        return google;
    }
    @Test void createsCustomerWithNormalizedEmailAndRandomEncodedPassword() {
        var google = identity("google-123", " CLIENTE@EXAMPLE.COM ", true);
        when(google.getFullName()).thenReturn("  Cliente Google  ");
        when(encoder.encode(anyString())).thenReturn("encoded");
        when(users.saveAndFlush(any(User.class))).thenAnswer(call -> call.getArgument(0));
        var account = service.resolve(google);
        var capture = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(capture.capture());
        var created = capture.getValue();
        assertEquals("cliente@example.com", created.getEmail());
        assertEquals("Cliente Google", created.getName());
        assertEquals("google-123", created.getGoogleSubject());
        assertEquals(Role.CUSTOMER, created.getRole());
        assertEquals("encoded", created.getPassword());
        assertEquals("cliente@example.com", account.getUsername());
        verify(encoder).encode(argThat(value -> value.length() > 60));
    }
    @Test void preservesLinkedAdministratorIdentityAndRole() {
        var stored = new User("Admin", "admin@example.com", "encoded");
        ReflectionTestUtils.setField(stored, "role", Role.ADMIN);
        when(users.findByGoogleSubject("google-123")).thenReturn(Optional.of(stored));
        var account = service.resolve(identity("google-123", "changed@example.com", true));
        assertEquals("admin@example.com", account.getUsername());
        assertTrue(account.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        verify(users, never()).saveAndFlush(any());
    }
    @Test void neverLinksAnExistingEmailAutomatically() {
        when(users.existsByEmail("cliente@example.com")).thenReturn(true);
        var error = assertThrows(OAuth2AuthenticationException.class,
                () -> service.resolve(identity("new", "cliente@example.com", true)));
        assertEquals("google_link_required", error.getError().getErrorCode());
        verify(users, never()).saveAndFlush(any());
    }
    @Test void rejectsDisabledLinkedAccounts() {
        var stored = new User("Cliente", "cliente@example.com", "encoded");
        ReflectionTestUtils.setField(stored, "enabled", false);
        when(users.findByGoogleSubject("google-123")).thenReturn(Optional.of(stored));
        assertThrows(OAuth2AuthenticationException.class,
                () -> service.resolve(identity("google-123", "cliente@example.com", true)));
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={" ", "\t"})
    void rejectsMissingSubject(String subject) {
        assertThrows(OAuth2AuthenticationException.class,
                () -> service.resolve(identity(subject, "cliente@example.com", true)));
        verifyNoInteractions(users);
    }
    @Test void rejectsOversizedSubject() {
        assertThrows(OAuth2AuthenticationException.class,
                () -> service.resolve(identity("x".repeat(256), "cliente@example.com", true)));
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={" "})
    void rejectsMissingEmail(String email) {
        assertThrows(OAuth2AuthenticationException.class,
                () -> service.resolve(identity("google-123", email, true)));
        verifyNoInteractions(users);
    }
    @Test void requiresExplicitEmailVerification() {
        for (Boolean value : new Boolean[]{false, null}) {
            assertThrows(OAuth2AuthenticationException.class,
                    () -> service.resolve(identity("google-123", "cliente@example.com", value)));
        }
        verifyNoInteractions(users);
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={" ", "x"})
    void suppliesFallbackNameOrUsesProvidedName(String name) {
        var google = identity("google-123", "cliente@example.com", true);
        when(google.getFullName()).thenReturn(name);
        when(encoder.encode(anyString())).thenReturn("encoded");
        when(users.saveAndFlush(any())).thenAnswer(call -> {
            User created = call.getArgument(0);
            assertEquals(name == null || name.isBlank() ? "Cliente" : name, created.getName());
            return created;
        });
        service.resolve(google);
    }
    @Test void limitsDisplayNameToDatabaseLength() {
        var google = identity("google-123", "cliente@example.com", true);
        when(google.getFullName()).thenReturn("x".repeat(300));
        when(encoder.encode(anyString())).thenReturn("encoded");
        when(users.saveAndFlush(any())).thenAnswer(call -> {
            User created = call.getArgument(0);
            assertEquals(255, created.getName().length());
            return created;
        });
        service.resolve(google);
    }
}
