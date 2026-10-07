package com.equisibe.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordConfigTest {

    @Test
    void shouldEncodeAndVerifyPassword() {
        var encoder = new PasswordConfig().passwordEncoder();
        String password = "ExamplePassword123";
        String hash = encoder.encode(password);

        assertNotEquals(password, hash);
        assertTrue(encoder.matches(password, hash));
        assertFalse(encoder.matches("WrongPassword123", hash));
    }
}