package com.equisibe.security;

import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class AccountUserDetails
        extends org.springframework.security.core.userdetails.User {

    private final long credentialVersion;

    public AccountUserDetails(com.equisibe.model.User user) {
        super(
                user.getEmail(),
                user.getPassword(),
                user.isEnabled(),
                true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        this.credentialVersion = user.getCredentialVersion();
    }

    public long getCredentialVersion() {
        return credentialVersion;
    }
}
