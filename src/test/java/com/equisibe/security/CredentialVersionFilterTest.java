package com.equisibe.security;

import com.equisibe.model.User;
import com.equisibe.repository.UserRepository;
import jakarta.servlet.FilterChain;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CredentialVersionFilterTest {
    final UserRepository users = mock(UserRepository.class);
    final CredentialVersionFilter filter = new CredentialVersionFilter(users);
    final FilterChain chain = mock(FilterChain.class);
    final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
    final MockHttpServletResponse response = new MockHttpServletResponse();
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    User authenticate() {
        var user = new User("Cliente", "client@example.com", "encoded");
        var principal = new AccountUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
        return user;
    }
    @Test void anonymousAndOtherPrincipalTypesContinueWithoutDatabaseLookup() throws Exception {
        SecurityContextHolder.clearContext();
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(users);
        reset(chain);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("other-principal", null, java.util.List.of()));
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(users);
    }
    @Test void currentCredentialsContinueWithSessionIntact() throws Exception {
        var user = authenticate();
        var session = request.getSession();
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        assertSame(session, request.getSession(false));
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }
    @ParameterizedTest @ValueSource(strings={"missing", "disabled", "password-changed"})
    void rejectsInvalidatedCredentialsWithoutCreatingSession(String reason) throws Exception {
        var user = authenticate();
        if (reason.equals("disabled")) ReflectionTestUtils.setField(user, "enabled", false);
        if (reason.equals("password-changed")) ReflectionTestUtils.setField(user, "credentialVersion", 1L);
        when(users.findByEmail(user.getEmail())).thenReturn(reason.equals("missing") ? Optional.empty() : Optional.of(user));
        filter.doFilter(request, response, chain);
        assertEquals(401, response.getStatus());
        assertNull(request.getSession(false));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(chain);
    }
    @Test void invalidatesExistingSessionWhenAccountDisappears() throws Exception {
        var user = authenticate();
        var session = (org.springframework.mock.web.MockHttpSession) request.getSession();
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.empty());
        filter.doFilter(request, response, chain);
        assertTrue(session.isInvalid());
        assertEquals(401, response.getStatus());
        verifyNoInteractions(chain);
    }
}
