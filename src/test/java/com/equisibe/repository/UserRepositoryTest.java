package com.equisibe.repository;

import com.equisibe.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldFindUserByEmail() {
        User user = new User("Andrea", "andrea@example.com", "test-password-hash");
        userRepository.saveAndFlush(user);

        var result = userRepository.findByEmail("andrea@example.com");

        assertTrue(result.isPresent());
        assertEquals(user.getId(), result.orElseThrow().getId());
    }
    @Test
void shouldDetectExistingEmail() {
    User user = new User("Andrea", "andrea@example.com", "test-password-hash");
    userRepository.saveAndFlush(user);

    assertTrue(userRepository.existsByEmail("andrea@example.com"));
}
}