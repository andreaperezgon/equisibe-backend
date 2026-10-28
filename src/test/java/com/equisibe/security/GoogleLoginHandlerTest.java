package com.equisibe.security;

import com.equisibe.model.User;
import com.equisibe.service.GoogleAccountService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GoogleLoginHandlerTest {
    GoogleAccountService accounts;
    GoogleLoginHandler handler;
    MockHttpServletRequest request;
    MockHttpServletResponse response;
    Authentication authentication;
    OidcUser identity;
    @BeforeEach void setup() {
        SecurityContextHolder.clearContext();
        accounts = mock(GoogleAccountService.class);
        handler = new GoogleLoginHandler(accounts, new HttpSessionSecurityContextRepository());
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        authentication = mock(Authentication.class);
        identity = mock(OidcUser.class);
        when(authentication.getPrincipal()).thenReturn(identity);
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void savesLocalSessionAndRemovesPreviousCsrfToken() throws Exception {
        var account = new AccountUserDetails(new User("Cliente", "cliente@example.com", "encoded"));
        when(accounts.resolve(identity)).thenReturn(account);
        var csrf = new HttpSessionCsrfTokenRepository();
        csrf.saveToken(new DefaultCsrfToken("X-CSRF", "_csrf", "old"), request, response);
        handler.onAuthenticationSuccess(request, response, authentication);
        assertEquals("http://localhost:5173/account", response.getRedirectedUrl());
        var current = SecurityContextHolder.getContext().getAuthentication();
        assertSame(account, current.getPrincipal());
        assertNull(current.getCredentials());
        assertTrue(current.isAuthenticated());
        assertSame(SecurityContextHolder.getContext(), request.getSession(false).getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
        assertNull(csrf.loadToken(request));
    }
    @ParameterizedTest @ValueSource(strings={"google_link_required", "google_identity_invalid"})
    void clearsSessionAndReturnsSafeErrorReason(String code) throws Exception {
        var session = request.getSession();
        when(accounts.resolve(identity)).thenThrow(new OAuth2AuthenticationException(new OAuth2Error(code)));
        handler.onAuthenticationSuccess(request, response, authentication);
        assertEquals("http://localhost:5173/login?google=" +
                (code.equals("google_link_required") ? "link-required" : "failed"), response.getRedirectedUrl());
        assertThrows(IllegalStateException.class, () -> session.getAttribute("anything"));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
    @Test void rejectsUnexpectedPrincipalWithoutCreatingSession() throws Exception {
        when(authentication.getPrincipal()).thenReturn("invalid-principal");
        handler.onAuthenticationSuccess(request, response, authentication);
        assertEquals("http://localhost:5173/login?google=failed", response.getRedirectedUrl());
        assertNull(request.getSession(false));
        verifyNoInteractions(accounts);
    }
    @Test void doesNotExposePersistenceErrors() throws Exception {
        when(accounts.resolve(identity)).thenThrow(new IllegalStateException("private detail"));
        handler.onAuthenticationSuccess(request, response, authentication);
        assertEquals("http://localhost:5173/login?google=failed", response.getRedirectedUrl());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
    @Test void clearsSessionWhenGoogleAuthenticationFails() throws Exception {
        request.getSession();
        handler.onAuthenticationFailure(request, response, new BadCredentialsException("denied"));
        assertEquals("http://localhost:5173/login?google=failed", response.getRedirectedUrl());
        assertNull(request.getSession(false));
    }
}
