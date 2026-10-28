package com.equisibe.controller;

import com.equisibe.model.*;
import com.equisibe.repository.*;
import com.equisibe.service.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminManagementIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired PromotionalPopupRepository popups;
    @Autowired ShopSettingsRepository settings;
    @Autowired AdminProductService productService;
    @Autowired PopupService popupService;
    @Autowired AdminOrderService orderService;
    @Autowired AdminReturnService returnService;
    MockMvc mvc;
    String adminEmail;
    String customerEmail;

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        adminEmail = UUID.randomUUID() + "@example.com";
        customerEmail = UUID.randomUUID() + "@example.com";
        var admin = new User("Admin", adminEmail, "encoded-test-password");
        ReflectionTestUtils.setField(admin, "role", Role.ADMIN);
        users.saveAndFlush(admin);
        users.saveAndFlush(new User("Cliente", customerEmail, "encoded-test-password"));
    }

    MockHttpServletRequestBuilder admin(MockHttpServletRequestBuilder request) {
        return request.with(user(adminEmail).roles("ADMIN")).with(csrf());
    }

    Product fixture() {
        var product = new Product("Fixture", "Algodón", new BigDecimal("40.00"),
                "/images/prenda.png", "Camisas");
        product.updateStock(ClothingSize.S, 5);
        return products.saveAndFlush(product);
    }

    @Test void createsReadsAndEditsProductsThroughRealServicesAndDatabase() throws Exception {
        String name = "Coverage-" + UUID.randomUUID();
        String payload = """
                {"name":"%s","description":"Algodón","price":40.00,
                 "imageUrl":"/images/prenda.png","category":"Camisas"}
                """.formatted(name);
        mvc.perform(admin(post("/api/admin/products")).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.stockBySize.S").value(0));
        var product = products.findAll().stream().filter(p -> name.equals(p.getName())).findFirst().orElseThrow();
        mvc.perform(admin(get("/api/admin/products"))).andExpect(status().isOk());
        mvc.perform(admin(get("/api/admin/products/" + product.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value(name));
        mvc.perform(admin(put("/api/admin/products/" + product.getId()))
                .contentType(MediaType.APPLICATION_JSON).content(payload.replace(name, "Editada")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Editada"));
        assertEquals("Editada", products.findById(product.getId()).orElseThrow().getName());
    }

    @Test void retiresAndReactivatesWithoutDeletingProduct() throws Exception {
        var product = fixture();
        for (boolean active : new boolean[]{false, true}) {
            mvc.perform(admin(put("/api/admin/products/" + product.getId() + "/active"))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"active\":" + active + "}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(active));
            assertEquals(active, products.findById(product.getId()).orElseThrow().isActive());
        }
    }

    @Test void updatesAllSizesAndRejectsIncompleteOrNegativeStock() throws Exception {
        var product = fixture();
        String path = "/api/admin/products/" + product.getId() + "/stock";
        mvc.perform(admin(put(path)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"stockBySize\":{\"S\":0,\"M\":4,\"L\":3,\"XL\":2}}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stockBySize.S").value(0));
        assertEquals(4, product.getStock(ClothingSize.M));
        mvc.perform(admin(put(path)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"stockBySize\":{\"S\":5}}"))
                .andExpect(status().isBadRequest());
        mvc.perform(admin(put(path)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"stockBySize\":{\"S\":-1,\"M\":4,\"L\":3,\"XL\":2}}"))
                .andExpect(status().isBadRequest());
        assertEquals(0, product.getStock(ClothingSize.S));
    }

    @Test void managesSalePricesAndRejectsPriceAboveRegularPrice() throws Exception {
        var product = fixture();
        String path = "/api/admin/products/" + product.getId() + "/sale";
        mvc.perform(admin(put(path)).contentType(MediaType.APPLICATION_JSON).content("{\"salePrice\":25.00}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.salePrice").value(25.0));
        mvc.perform(admin(put(path)).contentType(MediaType.APPLICATION_JSON).content("{\"salePrice\":50.00}"))
                .andExpect(status().isBadRequest());
        assertEquals(new BigDecimal("25.00"), product.getSalePrice());
        mvc.perform(admin(put(path)).contentType(MediaType.APPLICATION_JSON).content("{\"salePrice\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.salePrice").doesNotExist());
        assertNull(product.getSalePrice());
    }

    @ParameterizedTest @ValueSource(longs={0,-1,999999999})
    void rejectsInvalidAndMissingProductIdentifiers(long id) throws Exception {
        int expected = id <= 0 ? 400 : 404;
        mvc.perform(admin(get("/api/admin/products/" + id))).andExpect(status().is(expected));
        mvc.perform(admin(put("/api/admin/products/" + id + "/active"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().is(expected));
    }

    @Test void createsEditsActivatesAndDeletesPromotionalAnnouncement() throws Exception {
        popups.deleteAll();
        mvc.perform(get("/api/popups/active")).andExpect(status().isNoContent());
        String payload = "{\"title\":\"Bienvenida\",\"text\":\"Colección\",\"promoCode\":\" equisibe10 \"}";
        mvc.perform(admin(post("/api/admin/popups")).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.promoCode").value("EQUISIBE10"))
                .andExpect(jsonPath("$.active").value(false));
        var popup = popups.findAll().getFirst();
        String path = "/api/admin/popups/" + popup.getId();
        mvc.perform(admin(put(path)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Actualizado\",\"text\":\"Otro motivo\",\"promoCode\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Actualizado"));
        mvc.perform(admin(put(path + "/active")).contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/popups/active")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.title").value("Actualizado"));
        mvc.perform(admin(put(path + "/active")).contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk());
        mvc.perform(admin(get("/api/admin/popups"))).andExpect(status().isOk());
        mvc.perform(admin(delete(path))).andExpect(status().isNoContent());
        assertFalse(popups.existsById(popup.getId()));
    }

    @Test void createsExamplesOnceAndRejectsUnknownCodes() throws Exception {
        popups.deleteAll();
        mvc.perform(admin(post("/api/admin/popups/examples"))).andExpect(status().isOk());
        assertEquals(2, popups.count());
        mvc.perform(admin(post("/api/admin/popups/examples"))).andExpect(status().isOk());
        assertEquals(2, popups.count());
        mvc.perform(admin(post("/api/admin/popups")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Oferta\",\"text\":\"Texto\",\"promoCode\":\"INVALIDO\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(admin(delete("/api/admin/popups/0"))).andExpect(status().isBadRequest());
        mvc.perform(admin(delete("/api/admin/popups/999999999"))).andExpect(status().isNotFound());
    }

    @Test void keepsShopClosedAfterCountdownAndOpensOnlyAfterManualUpdate() throws Exception {
        settings.deleteAll();
        mvc.perform(get("/api/shop/status")).andExpect(status().isOk())
                .andExpect(jsonPath("$.closed").value(false));
        String payload = """
                {"closed":true,"title":"  Volvemos pronto  ","text":"  Nueva colección  ",
                 "countdownEnabled":true,"countdownTarget":"2020-01-01T00:00:00Z"}
                """;
        mvc.perform(admin(put("/api/admin/shop/settings")).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Volvemos pronto"));
        mvc.perform(get("/api/shop/status")).andExpect(status().isOk())
                .andExpect(jsonPath("$.closed").value(true));
        mvc.perform(admin(put("/api/admin/shop/settings")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"closed\":false,\"title\":\"Abierta\",\"text\":\"Texto\",\"countdownEnabled\":false,\"countdownTarget\":\"2020-01-01T00:00:00Z\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.countdownTarget").doesNotExist());
        assertFalse(settings.findById(1L).orElseThrow().isClosed());
        mvc.perform(admin(put("/api/admin/shop/settings")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"closed\":true,\"title\":\"Título\",\"text\":\"Texto\",\"countdownEnabled\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test void deniesCustomersEvenWhenTheyForgeAdminAuthority() {
        for (String email : new String[]{null, "", " ", "missing@example.com", customerEmail}) {
            HttpStatus expected = email == null || email.isBlank() ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
            assertEquals(expected, assertThrows(ResponseStatusException.class, () -> productService.getProducts(email)).getStatusCode());
            assertEquals(expected, assertThrows(ResponseStatusException.class, () -> popupService.getAll(email)).getStatusCode());
            assertEquals(expected, assertThrows(ResponseStatusException.class, () -> orderService.getOrders(email)).getStatusCode());
            assertEquals(expected, assertThrows(ResponseStatusException.class, () -> returnService.list(email)).getStatusCode());
        }
    }

    @Test void requiresAdminSessionAndCsrfForWrites() throws Exception {
        mvc.perform(get("/api/admin/products")).andExpect(status().is4xxClientError());
        mvc.perform(get("/api/admin/products").with(user(customerEmail).roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/popups").with(user(adminEmail).roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Oferta\",\"text\":\"Texto\"}"))
                .andExpect(status().isForbidden());
    }
}
