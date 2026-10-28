package com.equisibe.service;

import com.equisibe.model.*;
import com.equisibe.repository.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:stock_reservation_test;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000"
})
@ActiveProfiles("test")
class StockReservationIntegrationTest {

    @Autowired OrderPaymentService service;
    @Autowired ProductRepository products;
    @Autowired UserRepository users;
    @Autowired OrderRepository orders;
    @Autowired OrderPaymentRepository payments;
    @Autowired PlatformTransactionManager transactionManager;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    private TransactionTemplate transaction;
    private Long productId;
    private List<Long> orderIds;
    private List<Long> userIds;
    private List<String> emails;

    @BeforeEach
    void setUp() {
        transaction = new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> {
            Product product = new Product(
                    "Última camisa", "Prueba de stock", new BigDecimal("40.00"),
                    "/images/products/CAMISA.jpeg", "Camisas");
            product.updateStock(ClothingSize.M, 1);
            productId = products.saveAndFlush(product).getId();

            User first = users.saveAndFlush(new User(
                    "Cliente uno", UUID.randomUUID() + "@example.com", "test"));
            User second = users.saveAndFlush(new User(
                    "Cliente dos", UUID.randomUUID() + "@example.com", "test"));

            emails = List.of(first.getEmail(), second.getEmail());
            userIds = List.of(first.getId(), second.getId());
            orderIds = List.of(createOrder(first), createOrder(second));
        });
    }

    private Long createOrder(User user) {
        var address = new OrderShippingAddress(
                user.getName(), "600123456", "Calle Mayor 10", null,
                "Oviedo", "Asturias", "33001", "ES");

        return orders.saveAndFlush(new Order(
                user, address,
                List.of(new OrderItem(
                        productId, "Última camisa", ClothingSize.M, 1,
                        new BigDecimal("40.00"))),
                new BigDecimal("3.95")
        )).getId();
    }

    @AfterEach
    void cleanUp() {
        if (orderIds == null) return;

        transaction.executeWithoutResult(status -> {
            for (Long id : orderIds) {
                jdbc.update("delete from order_payments where order_id = ?", id);
                jdbc.update("delete from order_items where order_id = ?", id);
                jdbc.update("delete from orders where id = ?", id);
            }
            for (Long id : userIds) {
                jdbc.update("delete from users where id = ?", id);
            }
            jdbc.update("delete from product_size_stock where product_id = ?", productId);
            jdbc.update("delete from products where id = ?", productId);
        });
    }

    private int stock() {
        return transaction.execute(status ->
                products.findById(productId).orElseThrow().getStock(ClothingSize.M));
    }

    private OrderStatus orderStatus(Long id) {
        return transaction.execute(status ->
                orders.findById(id).orElseThrow().getStatus());
    }

    private boolean tryReserve(int customer, CountDownLatch ready, CountDownLatch start)
            throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("No se inició la prueba simultánea.");
        }

        try {
            service.prepare(emails.get(customer), orderIds.get(customer));
            return true;
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode().value() != 409) throw exception;
            return false;
        }
    }

    @Test
    void onlyOneCustomerCanReserveLastUnit() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<Boolean> first = executor.submit(() -> tryReserve(0, ready, start));
            Future<Boolean> second = executor.submit(() -> tryReserve(1, ready, start));

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            int successes = (first.get(20, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(20, TimeUnit.SECONDS) ? 1 : 0);

            assertEquals(1, successes);
            assertEquals(0, stock());
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void expiredSessionRestoresStockOnlyOnce() {
        Long id = orderIds.get(0);
        String session = "cs_test_" + UUID.randomUUID();
        Instant expiration = Instant.now().plusSeconds(3600);

        service.prepare(emails.get(0), id);
        service.attachSession(emails.get(0), id, session, expiration);
        assertEquals(0, stock());

        service.expirePayment(id, session, expiration);
        service.expirePayment(id, session, expiration);

        assertEquals(1, stock());
        assertEquals(OrderStatus.EXPIRED, orderStatus(id));

        assertThrows(ResponseStatusException.class,
                () -> service.confirmPayment(id, session, 4395L, "eur"));
        assertEquals(1, stock());
    }

    @Test
    void paidOrderDoesNotDeductTwiceOrReleaseSoldUnits() {
        Long id = orderIds.get(0);
        String session = "cs_test_" + UUID.randomUUID();
        Instant expiration = Instant.now().plusSeconds(3600);

        service.prepare(emails.get(0), id);
        service.attachSession(emails.get(0), id, session, expiration);

        service.confirmPayment(id, session, 4395L, "eur");
        service.confirmPayment(id, session, 4395L, "eur");
        service.expirePayment(id, session, expiration);

        assertEquals(0, stock());
        assertEquals(OrderStatus.PAID, orderStatus(id));
    }
}
