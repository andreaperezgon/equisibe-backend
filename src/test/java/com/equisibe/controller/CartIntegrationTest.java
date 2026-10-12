package com.equisibe.controller;

import com.equisibe.model.ClothingSize;
import com.equisibe.model.Product;
import com.equisibe.repository.ProductRepository;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CartIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ProductRepository productRepository;

    private MockMvc mockMvc;
    private Product product;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        product = new Product(
                "Camisa",
                "Camisa de algodón",
                new BigDecimal("49.90"),
                "/images/camisa.jpg",
                "Camisas"
        );

        product.updateStock(ClothingSize.S, 3);
        product.updateStock(ClothingSize.M, 0);

        product = productRepository.saveAndFlush(product);
    }

    @Test
    void validatesCartUsingDatabasePricesWithoutChangingStock()
            throws Exception {

        mockMvc.perform(request(item(product.getId(), "S", 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.items[0].unitPrice").value(49.90))
                .andExpect(jsonPath("$.items[0].lineTotal").value(99.80))
                .andExpect(jsonPath("$.subtotal").value(99.80))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.issues").isEmpty());

        var storedProduct = productRepository
                .findById(product.getId())
                .orElseThrow();

        assertEquals(3, storedProduct.getStock(ClothingSize.S));
    }

    @Test
    void combinesRepeatedProductAndSize() throws Exception {
        String items = item(product.getId(), "S", 1)
                + ","
                + item(product.getId(), "S", 2);

        mockMvc.perform(request(items))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.subtotal").value(149.70));
    }

    @Test
    void rejectsRepeatedLinesWhenCombinedQuantityExceedsStock()
            throws Exception {

        String items = item(product.getId(), "S", 2)
                + ","
                + item(product.getId(), "S", 2);

        mockMvc.perform(request(items))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.subtotal").doesNotExist())
                .andExpect(jsonPath("$.issues[0].code")
                        .value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.issues[0].availableStock")
                        .value(3));
    }

    @Test
    void rejectsSoldOutSize() throws Exception {
        mockMvc.perform(request(item(product.getId(), "M", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.issues[0].code")
                        .value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.issues[0].availableStock")
                        .value(0));
    }

    @Test
    void reportsInactiveAndMissingProducts() throws Exception {
        ReflectionTestUtils.setField(product, "active", false);
        productRepository.saveAndFlush(product);

        String items = item(product.getId(), "S", 1)
                + ","
                + item(Long.MAX_VALUE, "S", 1);

        mockMvc.perform(request(items))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.issues.length()").value(2))
                .andExpect(jsonPath("$.issues[0].code")
                        .value("PRODUCT_UNAVAILABLE"))
                .andExpect(jsonPath("$.issues[1].code")
                        .value("PRODUCT_UNAVAILABLE"));
    }

    @Test
    void acceptsOneSizeProducts() throws Exception {
        product.updateStock(ClothingSize.TU, 2);
        productRepository.saveAndFlush(product);

        mockMvc.perform(request(item(product.getId(), "TU", 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.items[0].size").value("TU"))
                .andExpect(jsonPath("$.subtotal").value(99.80));
    }

    @Test
    void rejectsRegularSizeForOneSizeProduct() throws Exception {
        product.updateStock(ClothingSize.TU, 2);
        productRepository.saveAndFlush(product);

        mockMvc.perform(request(item(product.getId(), "S", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.issues[0].code")
                        .value("INVALID_SIZE"));
    }

    @Test
    void rejectsOneSizeForRegularClothing() throws Exception {
        mockMvc.perform(request(item(product.getId(), "TU", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.issues[0].code")
                        .value("INVALID_SIZE"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "null",
            "{}",
            "{\"productId\":1,\"size\":\"S\",\"quantity\":0}",
            "{\"productId\":1,\"size\":\"S\",\"quantity\":-1}",
            "{\"productId\":0,\"size\":\"S\",\"quantity\":1}",
            "{\"productId\":1,\"size\":\"XXL\",\"quantity\":1}",
            "{\"productId\":1,\"size\":null,\"quantity\":1}",
            "{\"productId\":1,\"size\":\"S\",\"quantity\":null}"
    })
    void rejectsInvalidItems(String items) throws Exception {
        mockMvc.perform(request(items))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"items\":null}",
            "{\"items\":[]}"
    })
    void rejectsMissingOrEmptyCart(String body) throws Exception {
        mockMvc.perform(post("/api/cart/validate")
                        .with(user("cliente"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMoreThanOneHundredLines() throws Exception {
        String repeatedItem = item(product.getId(), "S", 1);

        String items = String.join(
                ",",
                java.util.Collections.nCopies(101, repeatedItem)
        );

        mockMvc.perform(request(items))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresLogin() throws Exception {
        String body = "{\"items\":["
                + item(product.getId(), "S", 1)
                + "]}";

        mockMvc.perform(post("/api/cart/validate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void requiresCsrfToken() throws Exception {
        String body = "{\"items\":["
                + item(product.getId(), "S", 1)
                + "]}";

        mockMvc.perform(post("/api/cart/validate")
                        .with(user("cliente"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    private String item(long productId, String size, long quantity) {
        return """
                {"productId":%d,"size":"%s","quantity":%d}
                """.formatted(productId, size, quantity);
    }

    private MockHttpServletRequestBuilder request(String items) {
        return post("/api/cart/validate")
                .with(user("cliente"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":[" + items + "]}");
    }
}