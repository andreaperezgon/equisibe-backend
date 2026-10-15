package com.equisibe.controller;

import com.equisibe.model.ClothingSize;
import com.equisibe.model.Product;
import com.equisibe.model.User;
import com.equisibe.repository.FavoriteRepository;
import com.equisibe.repository.ProductRepository;
import com.equisibe.repository.UserRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FavoriteIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private FavoriteRepository favoriteRepository;

    private MockMvc mvc;
    private User customer;
    private Product product;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity()).build();

        customer = saveUser();
        product = new Product(
                "Sudadera", "Prenda de prueba",
                new BigDecimal("54.90"), "/images/test.jpg", "Sudaderas"
        );
        product.updateStock(ClothingSize.L, 3);
        product = productRepository.saveAndFlush(product);
    }

    @Test
    void savesAndListsFavorite() throws Exception {
        addFavorite();

        mvc.perform(get("/api/favorites").with(user(customer.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(product.getId()))
                .andExpect(jsonPath("$[0].name").value("Sudadera"));
    }

    @Test
    void doesNotDuplicateFavorite() throws Exception {
        long before = favoriteRepository.count();
        addFavorite();
        addFavorite();
        assertEquals(before + 1, favoriteRepository.count());
    }

    @Test
    void removesFavoriteAndAllowsRepeatedRemoval() throws Exception {
        addFavorite();

        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(delete(path()).with(user(customer.getEmail()))
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }

        mvc.perform(get("/api/favorites").with(user(customer.getEmail())))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void cannotReadOrRemoveAnotherUsersFavorite() throws Exception {
        addFavorite();
        User other = saveUser();

        mvc.perform(get("/api/favorites")
                        .param("userId", customer.getId().toString())
                        .with(user(other.getEmail())))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        mvc.perform(delete(path()).with(user(other.getEmail())).with(csrf()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/favorites").with(user(customer.getEmail())))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void requiresLogin() throws Exception {
        mvc.perform(get("/api/favorites"))
                .andExpect(status().is4xxClientError());
        mvc.perform(put(path()).with(csrf()))
                .andExpect(status().is4xxClientError());
        mvc.perform(delete(path()).with(csrf()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void requiresCsrfToModifyFavorites() throws Exception {
        mvc.perform(put(path()).with(user(customer.getEmail())))
                .andExpect(status().isForbidden());
        mvc.perform(delete(path()).with(user(customer.getEmail())))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsInvalidProduct() throws Exception {
        mvc.perform(put("/api/favorites/0")
                        .with(user(customer.getEmail())).with(csrf()))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/favorites/" + Long.MAX_VALUE)
                        .with(user(customer.getEmail())).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void allowsFrontendCorsForBothOperations() throws Exception {
        for (String method : new String[]{"PUT", "DELETE"}) {
            mvc.perform(options(path())
                            .header("Origin", "http://localhost:5173")
                            .header("Access-Control-Request-Method", method)
                            .header("Access-Control-Request-Headers",
                                    "X-CSRF-TOKEN,Content-Type"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(
                            "Access-Control-Allow-Origin",
                            "http://localhost:5173"))
                    .andExpect(header().string(
                            "Access-Control-Allow-Methods",
                            containsString(method)));
        }
    }

    private void addFavorite() throws Exception {
        mvc.perform(put(path()).with(user(customer.getEmail())).with(csrf()))
                .andExpect(status().isNoContent());
    }

    private String path() {
        return "/api/favorites/" + product.getId();
    }

    private User saveUser() {
        return userRepository.saveAndFlush(new User(
                "Cliente", UUID.randomUUID() + "@example.com", "test-password"
        ));
    }
}
