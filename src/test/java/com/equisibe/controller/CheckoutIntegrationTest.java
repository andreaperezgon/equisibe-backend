package com.equisibe.controller;

import com.equisibe.model.ClothingSize;
import com.equisibe.model.Product;
import com.equisibe.model.ShippingAddress;
import com.equisibe.model.User;
import com.equisibe.repository.ProductRepository;
import com.equisibe.repository.ShippingAddressRepository;
import com.equisibe.repository.UserRepository;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
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
class CheckoutIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ShippingAddressRepository addressRepository;

    private MockMvc mockMvc;
    private User customer;
    private Product product;
    private ShippingAddress address;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        customer = userRepository.saveAndFlush(new User(
                "Cliente",
                UUID.randomUUID() + "@example.com",
                "test-password"
        ));

        product = new Product(
                "Camiseta",
                "Camiseta de prueba",
                new BigDecimal("40.00"),
                "/images/camiseta.jpg",
                "Camisetas"
        );

        product.updateStock(ClothingSize.S, 3);
        product = productRepository.saveAndFlush(product);
        address = saveAddress(customer, "ES");
    }

    @ParameterizedTest
    @CsvSource({
            "1, 40.00, 3.95, 43.95",
            "2, 80.00, 0.00, 80.00"
    })
    void validatesAddressAndCalculatesTotals(
            int quantity,
            double subtotal,
            double shipping,
            double total) throws Exception {

        mockMvc.perform(request(body(address.getId(), quantity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shippingAddress.id")
                        .value(address.getId().intValue()))
                .andExpect(jsonPath("$.shippingAddress.countryCode")
                        .value("ES"))
                .andExpect(jsonPath("$.cart.valid").value(true))
                .andExpect(jsonPath("$.cart.subtotal").value(subtotal))
                .andExpect(jsonPath("$.cart.shippingCost").value(shipping))
                .andExpect(jsonPath("$.cart.total").value(total))
                .andExpect(jsonPath("$.cart.currency").value("EUR"));

        assertEquals(
                3,
                productRepository.findById(product.getId())
                        .orElseThrow()
                        .getStock(ClothingSize.S)
        );
    }

    @Test
    void rejectsAnotherUsersAddress() throws Exception {
        var otherUser = userRepository.saveAndFlush(new User(
                "Otro cliente",
                UUID.randomUUID() + "@example.com",
                "test-password"
        ));

        var otherAddress = saveAddress(otherUser, "ES");

        mockMvc.perform(request(body(otherAddress.getId(), 1)))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsMissingAddress() throws Exception {
        mockMvc.perform(request(body(Long.MAX_VALUE, 1)))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsPreviouslySavedForeignAddress() throws Exception {
        var foreignAddress = saveAddress(customer, "FR");

        mockMvc.perform(request(body(foreignAddress.getId(), 1)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsStockIssuesWithoutTotals() throws Exception {
        mockMvc.perform(request(body(address.getId(), 4)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cart.valid").value(false))
                .andExpect(jsonPath("$.cart.issues[0].code")
                        .value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.cart.subtotal").doesNotExist())
                .andExpect(jsonPath("$.cart.shippingCost").doesNotExist())
                .andExpect(jsonPath("$.cart.total").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsInvalidQuantities(int quantity) throws Exception {
        mockMvc.perform(request(body(address.getId(), quantity)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"shippingAddressId\":1,\"items\":[]}",
            "{\"shippingAddressId\":1,\"items\":[null]}",
            "{\"shippingAddressId\":null,\"items\":[]}",
            "{\"shippingAddressId\":0,\"items\":[]}"
    })
    void rejectsIncompleteRequests(String body) throws Exception {
        mockMvc.perform(request(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresLogin() throws Exception {
        mockMvc.perform(post("/api/checkout/validate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(address.getId(), 1)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void requiresCsrfToken() throws Exception {
        mockMvc.perform(post("/api/checkout/validate")
                        .with(user(customer.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(address.getId(), 1)))
                .andExpect(status().isForbidden());
    }

    private MockHttpServletRequestBuilder request(String body) {
        return post("/api/checkout/validate")
                .with(user(customer.getEmail()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private String body(Long addressId, int quantity) {
        return """
                {
                  "shippingAddressId": %d,
                  "items": [
                    {
                      "productId": %d,
                      "size": "S",
                      "quantity": %d
                    }
                  ]
                }
                """.formatted(addressId, product.getId(), quantity);
    }

    private ShippingAddress saveAddress(User owner, String country) {
        return addressRepository.saveAndFlush(new ShippingAddress(
                owner,
                "Cliente de prueba",
                "600123456",
                "Calle Mayor 10",
                null,
                "Oviedo",
                "Asturias",
                "33001",
                country
        ));
    }
}