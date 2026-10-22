package com.equisibe.service;

import com.equisibe.model.User;
import com.equisibe.repository.UserRepository;
import com.equisibe.security.AccountUserDetails;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoogleAccountService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public GoogleAccountService(
            UserRepository users,
            PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AccountUserDetails resolve(OidcUser googleUser) {
        String subject = googleUser.getSubject();
        String email = googleUser.getEmail();

        if (subject == null || subject.isBlank() || subject.length() > 255
                || email == null || email.isBlank()
                || !Boolean.TRUE.equals(googleUser.getEmailVerified())) {
            throw failure("google_identity_invalid");
        }

        var linkedUser = users.findByGoogleSubject(subject);

        if (linkedUser.isPresent()) {
            return accountDetails(linkedUser.get());
        }

        email = email.trim().toLowerCase(Locale.ROOT);

        // Las cuentas existentes requieren vinculación explícita.
        if (users.existsByEmail(email)) {
            throw failure("google_link_required");
        }

        String name = googleUser.getFullName();
        if (name == null || name.isBlank()) {
            name = "Cliente";
        }
        name = name.trim();
        if (name.length() > 255) {
            name = name.substring(0, 255);
        }

        // No se utiliza la contraseña de Google ni una contraseña compartida.
        String randomPassword = UUID.randomUUID() + "-" + UUID.randomUUID();
        User user = new User(
                name, email, passwordEncoder.encode(randomPassword));
        user.linkGoogleSubject(subject);

        return accountDetails(users.saveAndFlush(user));
    }

    private AccountUserDetails accountDetails(User user) {
        if (!user.isEnabled()) {
            throw failure("google_account_unavailable");
        }
        return new AccountUserDetails(user);
    }

    private OAuth2AuthenticationException failure(String code) {
        return new OAuth2AuthenticationException(new OAuth2Error(code));
    }
}
