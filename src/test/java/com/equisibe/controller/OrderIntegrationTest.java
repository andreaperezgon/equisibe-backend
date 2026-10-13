package com.equisibe.controller;

import com.equisibe.model.ClothingSize;
import com.equisibe.model.Product;
import com.equisibe.model.ShippingAddress;
import com.equisibe.model.User;
import com.equisibe.repository.OrderRepository;
import com.equisibe.repository.ProductRepository;
import com.equisibe.repository.ShippingAddressRepository;
import com.equisibe.repository.UserRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ShippingAddressRepository addressRepository;

    @Autowired
    private OrderRepository orderRepository;

    private MockMvc mockMvc;
    private User customer;
    private Product product;
    private ShippingAddress address;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        customer = saveUser();

        product = new Product(
                "Camiseta",
                "Camiseta de prueba",
                new BigDecimal("40.00"),
                "/images/camiseta.jpg",
                "Camisetas"
        );
        product.updateStock(ClothingSize.S, 3);
        product = productRepository.saveAndFlush(product);

        address = addressRepository.saveAndFlush(new ShippingAddress(
                customer,
                "Cliente de prueba",
                "600123456",
                "Calle Mayor 10",
                null,
                "Oviedo",
                "Asturias",
                "33001",
                "ES"
        ));
    }

    @ParameterizedTest
    @CsvSource({
            "1, 40.00, 3.95, 43.95",
            "2, 80.00, 0.00, 80.00"
    })
    void createsAndReadsOrder(
            int quantity,
            double subtotal,
            double shipping,
            double total) throws Exception {

        String location = createOrder(quantity);

        mockMvc.perform(get(location)
                        .with(user(customer.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.subtotal").value(subtotal))
                .andExpect(jsonPath("$.shippingCost").value(shipping))
                .andExpect(jsonPath("$.total").value(total))
                .andExpect(jsonPath("$.items[0].productName")
                        .value("Camiseta"))
                .andExpect(jsonPath("$.items[0].size").value("S"))
                .andExpect(jsonPath("$.items[0].quantity").value(quantity))
                .andExpect(jsonPath("$.shippingAddress.city")
                        .value("Oviedo"))
                .andExpect(jsonPath("$.shippingAddress.countryCode")
                        .value("ES"));
    }

    @Test
    void rejectsAccessToAnotherCustomersOrder() throws Exception {
        String location = createOrder(1);
        User other = saveUser();

        mockMvc.perform(get(location)
                        .with(user(other.getEmail())))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsAnotherCustomersAddressWithoutSaving() throws Exception {
        User other = saveUser();
        long countBefore = orderRepository.count();

        mockMvc.perform(post("/api/orders")
                        .with(user(other.getEmail()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(1)))
                .andExpect(status().isNotFound());

        assertEquals(countBefore, orderRepository.count());
    }

    @Test
    void rejectsInsufficientStockWithoutSaving() throws Exception {
        long countBefore = orderRepository.count();

        mockMvc.perform(request(4))
                .andExpect(status().isConflict());

        assertEquals(countBefore, orderRepository.count());
    }

    @Test
    void rejectsInvalidQuantityWithoutSaving() throws Exception {
        long countBefore = orderRepository.count();

        mockMvc.perform(request(0))
                .andExpect(status().isBadRequest());

        assertEquals(countBefore, orderRepository.count());
    }

    @Test
    void requiresSessionToCreateOrder() throws Exception {
        long countBefore = orderRepository.count();

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(1)))
                .andExpect(status().is4xxClientError());

        assertEquals(countBefore, orderRepository.count());
    }

    @Test
    void requiresSessionToReadOrder() throws Exception {
        String location = createOrder(1);

        mockMvc.perform(get(location))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void requiresCsrfTokenToCreateOrder() throws Exception {
        long countBefore = orderRepository.count();

        mockMvc.perform(post("/api/orders")
                        .with(user(customer.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(1)))
                .andExpect(status().isForbidden());

        assertEquals(countBefore, orderRepository.count());
    }

    private String createOrder(int quantity) throws Exception {
        var result = mockMvc.perform(request(quantity))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(header().exists("Location"))
                .andReturn();

        String location = result.getResponse().getHeader("Location");
        assertNotNull(location);
        return location;
    }

    private MockHttpServletRequestBuilder request(int quantity) {
        return post("/api/orders")
                .with(user(customer.getEmail()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(quantity));
    }

    private String body(int quantity) {
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
                """.formatted(address.getId(), product.getId(), quantity);
    }

    private User saveUser() {
        return userRepository.saveAndFlush(new User(
                "Cliente de prueba",
                UUID.randomUUID() + "@example.com",
                "test-password"
        ));
    }
}