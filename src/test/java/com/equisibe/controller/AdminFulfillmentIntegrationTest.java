package com.equisibe.controller;

import com.equisibe.model.*;
import com.equisibe.repository.*;
import com.equisibe.service.AdminOrderService;
import com.equisibe.service.AdminReturnService;
import java.math.BigDecimal;
import java.util.List;
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
class AdminFulfillmentIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired OrderRepository orders;
    @Autowired ReturnRequestRepository requests;
    @Autowired AdminOrderService orderService;
    @Autowired AdminReturnService returnService;
    MockMvc mvc;
    User customer;
    String adminEmail;

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        adminEmail = UUID.randomUUID() + "@example.com";
        var admin = new User("Admin", adminEmail, "encoded");
        ReflectionTestUtils.setField(admin, "role", Role.ADMIN);
        users.saveAndFlush(admin);
        customer = users.saveAndFlush(new User("Cliente", UUID.randomUUID() + "@example.com", "encoded"));
    }
    MockHttpServletRequestBuilder admin(MockHttpServletRequestBuilder request) {
        return request.with(user(adminEmail).roles("ADMIN")).with(csrf());
    }
    Order order(boolean paid) {
        var address = new OrderShippingAddress("Cliente", "600000000", "Calle de prueba 1", null,
                "Oviedo", "Asturias", "33001", "ES");
        var item = new OrderItem(1L, "Camisa", ClothingSize.S, 2L, new BigDecimal("40.00"));
        var order = new Order(customer, address, List.of(item), new BigDecimal("3.95"));
        if (paid) order.markPaid();
        return orders.saveAndFlush(order);
    }
    ReturnRequest request(Order order) {
        return requests.saveAndFlush(new ReturnRequest(UUID.randomUUID(), order,
                ReturnType.WITHDRAWAL, "Devolución", List.of(new ReturnItem(0, 1))));
    }
    void updateOrder(Long id, String status, int expected) throws Exception {
        mvc.perform(admin(put("/api/admin/orders/" + id + "/status"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}"))
                .andExpect(status().is(expected));
    }
    void updateReturn(Long id, String status, String note, int expected) throws Exception {
        mvc.perform(admin(put("/api/admin/returns/" + id + "/status"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + status + "\",\"note\":\"" + note + "\"}"))
                .andExpect(status().is(expected));
    }

    @Test void listsOnlyPaidOrdersAndIncludesCustomerAndShippingDetails() throws Exception {
        var paid = order(true);
        var pending = order(false);
        mvc.perform(admin(get("/api/admin/orders"))).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + paid.getId() + ")]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.id == " + pending.getId() + ")]").isEmpty());
        mvc.perform(admin(get("/api/admin/orders/" + paid.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.customerEmail").value(customer.getEmail()))
                .andExpect(jsonPath("$.shippingAddress.city").value("Oviedo"))
                .andExpect(jsonPath("$.items[0].productName").value("Camisa"));
    }

    @Test void advancesPaidOrderThroughPreparationShippingAndDelivery() throws Exception {
        var paid = order(true);
        updateOrder(paid.getId(), "DELIVERED", 409);
        updateOrder(paid.getId(), "PREPARING", 200);
        updateOrder(paid.getId(), "PREPARING", 200);
        updateOrder(paid.getId(), "SHIPPED", 200);
        updateOrder(paid.getId(), "DELIVERED", 200);
        assertEquals(OrderStatus.DELIVERED, orders.findById(paid.getId()).orElseThrow().getStatus());
        updateOrder(paid.getId(), "PREPARING", 409);
        updateOrder(paid.getId(), "PAID", 409);
    }

    @Test void doesNotPrepareAnUnpaidOrder() throws Exception {
        var pending = order(false);
        updateOrder(pending.getId(), "PREPARING", 409);
        assertEquals(OrderStatus.PENDING_PAYMENT, pending.getStatus());
    }

    @ParameterizedTest @ValueSource(longs={0,-1,999999999})
    void rejectsInvalidAndMissingOrderIds(long id) throws Exception {
        int expected = id <= 0 ? 400 : 404;
        mvc.perform(admin(get("/api/admin/orders/" + id))).andExpect(status().is(expected));
        updateOrder(id, "PREPARING", expected);
    }

    @Test void rejectsMissingOrderIdentifierInService() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> orderService.getOrder(adminEmail, null)).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> orderService.updateStatus(adminEmail, null, OrderStatus.PREPARING)).getStatusCode());
    }

    @Test void managesReturnUntilClosedAndExposesResponseToItsOwner() throws Exception {
        var paid = order(true);
        var request = request(paid);
        mvc.perform(admin(get("/api/admin/returns"))).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.request.id == " + request.getId() + ")]").isNotEmpty());
        for (String state : new String[]{"IN_REVIEW", "AWAITING_ITEMS", "RECEIVED", "CLOSED"}) {
            updateReturn(request.getId(), state, "Respuesta del taller", 200);
            assertEquals(ReturnStatus.valueOf(state), requests.findById(request.getId()).orElseThrow().getStatus());
        }
        mvc.perform(get("/api/orders/" + paid.getId() + "/returns")
                .with(user(customer.getEmail()).roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("CLOSED"))
                .andExpect(jsonPath("$[0].adminNote").value("Respuesta del taller"));
        assertEquals(OrderStatus.PAID, paid.getStatus());
    }

    @Test void rejectsSkippingReturnStagesAndRequiresReasonForRejection() throws Exception {
        var request = request(order(true));
        updateReturn(request.getId(), "CLOSED", "Resuelta", 409);
        updateReturn(request.getId(), "REJECTED", "", 409);
        updateReturn(request.getId(), "REJECTED", "Motivo de rechazo", 200);
        updateReturn(request.getId(), "IN_REVIEW", "Reabrir", 409);
        assertEquals(ReturnStatus.REJECTED, request.getStatus());
    }

    @ParameterizedTest @ValueSource(longs={0,-1,999999999})
    void rejectsInvalidAndMissingReturnIds(long id) throws Exception {
        updateReturn(id, "IN_REVIEW", "Respuesta", id <= 0 ? 400 : 404);
    }

    @Test void rejectsNullReturnIdentifierAndDisabledAdministrator() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> returnService.update(adminEmail, null, ReturnStatus.IN_REVIEW, "")).getStatusCode());
        var admin = users.findByEmail(adminEmail).orElseThrow();
        ReflectionTestUtils.setField(admin, "enabled", false);
        users.saveAndFlush(admin);
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                () -> returnService.list(adminEmail)).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                () -> orderService.getOrders(adminEmail)).getStatusCode());
    }
}

