package com.equisibe.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.CUSTOMER;

    @Column(nullable = false)
    private boolean enabled = true;


    @Column(nullable = false, columnDefinition = "bigint default 0")
    private long credentialVersion = 0;

    public long getCredentialVersion() {
        return credentialVersion;
    }


    @Column(name = "google_subject", unique = true, length = 255)
    private String googleSubject;

    public String getGoogleSubject() {
        return googleSubject;
    }

    public void linkGoogleSubject(String subject) {
        if (subject == null || subject.isBlank() || subject.length() > 255) {
            throw new IllegalArgumentException("La identidad de Google no es válida.");
        }
        if (googleSubject != null && !googleSubject.equals(subject)) {
            throw new IllegalStateException("La cuenta ya está vinculada a otra identidad de Google.");
        }
        googleSubject = subject;
    }

    protected User() {
    }

    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void changePassword(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía.");
        }
        this.password = encodedPassword;
        this.credentialVersion++;
    }
}