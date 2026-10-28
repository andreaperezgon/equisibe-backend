package com.equisibe.security;

import com.equisibe.service.GoogleAccountService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.stereotype.Component;

@Component
public class GoogleLoginHandler implements
        AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private final GoogleAccountService accounts;
    private final SecurityContextRepository contexts;

    public GoogleLoginHandler(
            GoogleAccountService accounts,
            SecurityContextRepository contexts) {
        this.accounts = accounts;
        this.contexts = contexts;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        try {
            if (!(authentication.getPrincipal() instanceof OidcUser googleUser)) {
                reject(request, response, "failed");
                return;
            }

            AccountUserDetails account = accounts.resolve(googleUser);
            var localAuthentication =
                    UsernamePasswordAuthenticationToken.authenticated(
                            account, null, account.getAuthorities());

            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(localAuthentication);
            SecurityContextHolder.setContext(context);
            contexts.saveContext(context, request, response);

            new HttpSessionCsrfTokenRepository()
                    .saveToken(null, request, response);
            new HttpSessionRequestCache().removeRequest(request, response);

            response.sendRedirect("http://localhost:5173/account");
        } catch (OAuth2AuthenticationException exception) {
            String reason = "google_link_required".equals(
                    exception.getError().getErrorCode())
                    ? "link-required" : "failed";
            reject(request, response, reason);
        } catch (RuntimeException exception) {
            reject(request, response, "failed");
        }
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        reject(request, response, "failed");
    }

    private void reject(
            HttpServletRequest request,
            HttpServletResponse response,
            String reason) throws IOException {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        response.sendRedirect(
                "http://localhost:5173/login?google=" + reason);
    }
}
