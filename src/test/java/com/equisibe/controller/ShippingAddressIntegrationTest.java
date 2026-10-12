package com.equisibe.controller;

import com.equisibe.model.ShippingAddress;
import com.equisibe.model.User;
import com.equisibe.repository.ShippingAddressRepository;
import com.equisibe.repository.UserRepository;

import java.util.UUID;

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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ShippingAddressIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShippingAddressRepository addressRepository;

    private MockMvc mockMvc;
    private User owner;
    private User otherUser;

    private static final String VALID_BODY = """
            {
              "recipientName": "Andrea Pérez",
              "phone": "+34 600123456",
              "addressLine1": "Calle Mayor 10",
              "addressLine2": "2 B",
              "city": "Oviedo",
              "province": "Asturias",
              "postalCode": "33001",
              "countryCode": "ES"
            }
            """;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        String suffix = UUID.randomUUID().toString();

        owner = userRepository.saveAndFlush(new User(
                "Andrea",
                "andrea-" + suffix + "@example.com",
                "test-password"
        ));

        otherUser = userRepository.saveAndFlush(new User(
                "Otro usuario",
                "other-" + suffix + "@example.com",
                "test-password"
        ));
    }

    @Test
    void returnsEmptyListWhenUserHasNoAddresses() throws Exception {
        mockMvc.perform(get("/api/addresses")
                        .with(user(owner.getEmail())))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void listsOnlyCurrentUserAddresses() throws Exception {
        var ownAddress = saveAddress(owner, "Calle propia 1");
        saveAddress(otherUser, "Calle ajena 2");

        mockMvc.perform(get("/api/addresses")
                        .with(user(owner.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id")
                        .value(ownAddress.getId().intValue()))
                .andExpect(jsonPath("$[0].addressLine1")
                        .value("Calle propia 1"))
                .andExpect(jsonPath("$[0].user").doesNotExist())
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void createsAddressLinkedToCurrentUser() throws Exception {
        mockMvc.perform(post("/api/addresses")
                        .with(user(owner.getEmail()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.recipientName")
                        .value("Andrea Pérez"))
                .andExpect(jsonPath("$.city").value("Oviedo"))
                .andExpect(jsonPath("$.countryCode").value("ES"));

        var addresses = addressRepository
                .findByUser_IdOrderByIdDesc(owner.getId());

        assertEquals(1, addresses.size());
        assertEquals(owner.getId(), addresses.get(0).getUser().getId());
        assertEquals("Calle Mayor 10", addresses.get(0).getAddressLine1());

        assertEquals(
                0,
                addressRepository
                        .findByUser_IdOrderByIdDesc(otherUser.getId())
                        .size()
        );
    }

    @Test
    void acceptsBlankOptionalFieldsAndTrimsRequiredFields()
            throws Exception {

        String body = VALID_BODY
                .replace("Andrea Pérez", "  Andrea Pérez  ")
                .replace("\"2 B\"", "\"   \"")
                .replace("\"Asturias\"", "null");

        mockMvc.perform(post("/api/addresses")
                        .with(user(owner.getEmail()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recipientName")
                        .value("Andrea Pérez"));

        var address = addressRepository
                .findByUser_IdOrderByIdDesc(owner.getId())
                .getFirst();

        assertNull(address.getAddressLine2());
        assertNull(address.getProvince());
    }

    @Test
    void getsOwnAddress() throws Exception {
        var address = saveAddress(owner, "Calle propia 1");

        mockMvc.perform(get("/api/addresses/" + address.getId())
                        .with(user(owner.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(address.getId().intValue()))
                .andExpect(jsonPath("$.addressLine1")
                        .value("Calle propia 1"));
    }

    @Test
    void cannotReadAnotherUsersAddress() throws Exception {
        var address = saveAddress(otherUser, "Calle privada 2");

        mockMvc.perform(get("/api/addresses/" + address.getId())
                        .with(user(owner.getEmail())))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatesOwnAddressWithoutChangingOwner() throws Exception {
        var address = saveAddress(owner, "Dirección antigua");

        mockMvc.perform(put("/api/addresses/" + address.getId())
                        .with(user(owner.getEmail()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(address.getId().intValue()))
                .andExpect(jsonPath("$.addressLine1")
                        .value("Calle Mayor 10"));

        addressRepository.flush();

        var saved = addressRepository
                .findById(address.getId())
                .orElseThrow();

        assertEquals("Calle Mayor 10", saved.getAddressLine1());
        assertEquals(owner.getId(), saved.getUser().getId());
    }

    @Test
    void cannotUpdateAnotherUsersAddress() throws Exception {
        var address = saveAddress(otherUser, "Dirección privada");

        mockMvc.perform(put("/api/addresses/" + address.getId())
                        .with(user(owner.getEmail()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound());

        var saved = addressRepository
                .findById(address.getId())
                .orElseThrow();

        assertEquals("Dirección privada", saved.getAddressLine1());
        assertEquals(otherUser.getId(), saved.getUser().getId());
    }

    @Test
    void returnsNotFoundForMissingAddress() throws Exception {
        mockMvc.perform(get("/api/addresses/" + Long.MAX_VALUE)
                        .with(user(owner.getEmail())))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/addresses/" + Long.MAX_VALUE)
                        .with(user(owner.getEmail()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "recipientName",
            "phone",
            "addressLine1",
            "city",
            "postalCode",
            "countryCode"
    })
    void rejectsBlankRequiredFields(String field) throws Exception {
        String body = VALID_BODY.replaceAll(
                "\"" + field + "\"\\s*:\\s*\"[^\"]*\"",
                "\"" + field + "\": \"\""
        );

        mockMvc.perform(post("/api/addresses")
                        .with(user(owner.getEmail()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        assertEquals(
                0,
                addressRepository
                        .findByUser_IdOrderByIdDesc(owner.getId())
                        .size()
        );
    }

    @Test
    void rejectsMalformedPhone() throws Exception {
        String body = VALID_BODY.replace("+34 600123456", "abcdefghi");

        mockMvc.perform(post("/api/addresses")
                        .with(user(owner.getEmail()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInvalidUpdateWithoutChangingAddress() throws Exception {
        var address = saveAddress(owner, "Dirección original");
        String body = VALID_BODY.replace("\"ES\"", "\"España\"");

        mockMvc.perform(put("/api/addresses/" + address.getId())
                        .with(user(owner.getEmail()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        assertEquals(
                "Dirección original",
                addressRepository.findById(address.getId())
                        .orElseThrow()
                        .getAddressLine1()
        );
    }

    @Test
    void rejectsDisabledUser() throws Exception {
        ReflectionTestUtils.setField(owner, "enabled", false);
        userRepository.saveAndFlush(owner);

        mockMvc.perform(get("/api/addresses")
                        .with(user(owner.getEmail())))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/addresses")
                        .with(user(owner.getEmail()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requiresLogin() throws Exception {
        mockMvc.perform(get("/api/addresses"))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(post("/api/addresses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void requiresCsrfForCreatingAndUpdating() throws Exception {
        var address = saveAddress(owner, "Dirección original");

        mockMvc.perform(post("/api/addresses")
                        .with(user(owner.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/addresses/" + address.getId())
                        .with(user(owner.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    private ShippingAddress saveAddress(User user, String street) {
        return addressRepository.saveAndFlush(new ShippingAddress(
                user,
                user.getName(),
                "+34 600123456",
                street,
                null,
                "Oviedo",
                "Asturias",
                "33001",
                "ES"
        ));
    }
}