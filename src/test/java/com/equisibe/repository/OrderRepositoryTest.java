package com.equisibe.repository;

import com.equisibe.model.ClothingSize;
import com.equisibe.model.Order;
import com.equisibe.model.OrderItem;
import com.equisibe.model.OrderShippingAddress;
import com.equisibe.model.OrderStatus;
import com.equisibe.model.User;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsOrderWithItemsAddressAndTotals() {
        User user = saveUser("cliente@example.com");
        Order saved = orderRepository.saveAndFlush(createOrder(user));

        Long orderId = saved.getId();
        Long userId = user.getId();

        entityManager.clear();

        Order loaded = orderRepository
                .findByIdAndUser_Id(orderId, userId)
                .orElseThrow();

        assertEquals(OrderStatus.PENDING_PAYMENT, loaded.getStatus());
        assertEquals("EUR", loaded.getCurrency());
        assertNotNull(loaded.getCreatedAt());
        assertEquals(userId, loaded.getUser().getId());

        assertEquals(new BigDecimal("54.90"), loaded.getSubtotal());
        assertEquals(new BigDecimal("3.95"), loaded.getShippingCost());
        assertEquals(new BigDecimal("58.85"), loaded.getTotal());

        assertEquals(1, loaded.getItems().size());

        OrderItem item = loaded.getItems().getFirst();
        assertEquals(Long.valueOf(1L), item.getProductId());
        assertEquals("Sudadera", item.getProductName());
        assertEquals(ClothingSize.L, item.getSize());
        assertEquals(1L, item.getQuantity());
        assertEquals(new BigDecimal("54.90"), item.getUnitPrice());
        assertEquals(new BigDecimal("54.90"), item.getLineTotal());

        OrderShippingAddress address = loaded.getShippingAddress();
        assertEquals("Cliente Prueba", address.getRecipientName());
        assertEquals("600123456", address.getPhone());
        assertEquals("Calle Ejemplo 1", address.getAddressLine1());
        assertEquals("2B", address.getAddressLine2());
        assertEquals("Oviedo", address.getCity());
        assertEquals("Asturias", address.getProvince());
        assertEquals("33001", address.getPostalCode());
        assertEquals("ES", address.getCountryCode());
    }

    @Test
    void doesNotFindOrderForAnotherCustomer() {
        User owner = saveUser("propietario@example.com");
        User other = saveUser("otro@example.com");

        Order saved = orderRepository.saveAndFlush(createOrder(owner));
        Long orderId = saved.getId();
        Long otherId = other.getId();

        entityManager.clear();

        assertTrue(
                orderRepository.findByIdAndUser_Id(orderId, otherId)
                        .isEmpty()
        );
    }

    @Test
    void listsOnlyOrdersBelongingToCustomer() {
        User owner = saveUser("propietario@example.com");
        User other = saveUser("otro@example.com");

        Order ownOrder = orderRepository.saveAndFlush(createOrder(owner));
        orderRepository.saveAndFlush(createOrder(other));

        Long ownerId = owner.getId();
        Long ownOrderId = ownOrder.getId();

        entityManager.clear();

        List<Order> orders = orderRepository
                .findByUser_IdOrderByCreatedAtDescIdDesc(ownerId);

        assertEquals(1, orders.size());
        assertEquals(ownOrderId, orders.getFirst().getId());
    }

    private User saveUser(String email) {
        return userRepository.saveAndFlush(
                new User("Cliente Prueba", email, "password-test")
        );
    }

    private Order createOrder(User user) {
        OrderShippingAddress address = new OrderShippingAddress(
                "Cliente Prueba",
                "600123456",
                "Calle Ejemplo 1",
                "2B",
                "Oviedo",
                "Asturias",
                "33001",
                "ES"
        );

        OrderItem item = new OrderItem(
                1L,
                "Sudadera",
                ClothingSize.L,
                1L,
                new BigDecimal("54.90")
        );

        return new Order(
                user,
                address,
                List.of(item),
                new BigDecimal("3.95")
        );
    }
}