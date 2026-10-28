package com.equisibe.config;

import com.equisibe.model.ClothingSize;
import com.equisibe.model.Order;
import com.equisibe.model.OrderItem;
import com.equisibe.model.OrderShippingAddress;
import com.equisibe.model.ReturnItem;
import com.equisibe.model.ReturnRequest;
import com.equisibe.model.ReturnType;
import com.equisibe.repository.OrderRepository;
import com.equisibe.repository.ProductRepository;
import com.equisibe.repository.ReturnRequestRepository;
import com.equisibe.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Preparación de escenarios ficticios; no se incluye en el artefacto de producción. */
@RestController
@Profile("e2e")
public class E2eScenarioController {
    private final UserRepository users;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final ReturnRequestRepository returns;

    public E2eScenarioController(UserRepository users, ProductRepository products,
            OrderRepository orders, ReturnRequestRepository returns) {
        this.users = users;
        this.products = products;
        this.orders = orders;
        this.returns = returns;
    }
    @PostMapping("/api/admin/e2e/orders")
    @Transactional
    public Map<String, Long> newPaidOrder() {
        var customer = users.findByEmail("e2e-customer@example.test").orElseThrow();
        var product = products.findAll().stream().filter(item -> "Camisa E2E".equals(item.getName()))
                .findFirst().orElseThrow();
        var address = new OrderShippingAddress("Cliente E2E", "600000000", "Calle de prueba 1",
                null, "Oviedo", "Asturias", "33001", "ES");
        var order = new Order(customer, address,
                List.of(new OrderItem(product.getId(), product.getName(), ClothingSize.S, 1,
                        new BigDecimal("40.00"))), new BigDecimal("3.95"));
        order.markPaid();
        orders.saveAndFlush(order);
        var request = returns.saveAndFlush(new ReturnRequest(UUID.randomUUID(), order, ReturnType.WITHDRAWAL,
                "Solicitud ficticia E2E", List.of(new ReturnItem(0, 1))));
        return Map.of("orderId", order.getId(), "returnId", request.getId());
    }
}
