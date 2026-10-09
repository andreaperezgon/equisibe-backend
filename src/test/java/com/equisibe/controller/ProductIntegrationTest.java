package com.equisibe.controller;

import com.equisibe.model.Product;
import com.equisibe.repository.ProductRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ProductRepository productRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void shouldAllowAnonymousAccessToCatalog() throws Exception {
        var product = saveProduct();

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id")
                        .value(hasItem(product.getId().intValue())))
                .andExpect(jsonPath("$[*].name")
                        .value(hasItem("Camisa blanca")));
    }

    @Test
    void shouldAllowAnonymousAccessToProductDetails() throws Exception {
        var product = saveProduct();

        mockMvc.perform(get("/api/products/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(product.getId().intValue()))
                .andExpect(jsonPath("$.name").value("Camisa blanca"))
                .andExpect(jsonPath("$.price").value(49.90));
    }

    @Test
    void shouldReturnNotFoundForMissingProduct() throws Exception {
        mockMvc.perform(get("/api/products/" + Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    private Product saveProduct() {
        return productRepository.saveAndFlush(new Product(
                "Camisa blanca",
                "Camisa de algodón.",
                new BigDecimal("49.90"),
                "/images/white-shirt.jpg",
                "Camisas"
        ));
    }
}