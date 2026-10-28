package com.equisibe.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class OrderBoundaryTest {
    final User customer = new User("Cliente", "client@example.com", "encoded");
    final OrderShippingAddress address = new OrderShippingAddress("Cliente", "600000000", "Calle 1",
            null, "Oviedo", "Asturias", "33001", "ES");
    OrderItem item() { return new OrderItem(1L, "Camisa", ClothingSize.S, 2, new BigDecimal("40.00")); }
    Order order() { return new Order(customer, address, List.of(item()), new BigDecimal("3.95")); }

    @ParameterizedTest @NullSource @ValueSource(longs={0,-1})
    void productIdentifierMustBePositive(Long id) {
        assertThrows(IllegalArgumentException.class,
                () -> new OrderItem(id, "Camisa", ClothingSize.S, 1, BigDecimal.ONE));
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={" ", "\t"})
    void productNameCannotBeMissing(String name) {
        assertThrows(IllegalArgumentException.class,
                () -> new OrderItem(1L, name, ClothingSize.S, 1, BigDecimal.ONE));
    }
    @Test void productNameHasLengthLimitAndIsTrimmed() {
        assertThrows(IllegalArgumentException.class,
                () -> new OrderItem(1L, "x".repeat(101), ClothingSize.S, 1, BigDecimal.ONE));
        assertEquals("Camisa", new OrderItem(1L, " Camisa ", ClothingSize.S, 1, BigDecimal.ONE).getProductName());
    }
    @ParameterizedTest @ValueSource(longs={0,-1,Long.MIN_VALUE})
    void rejectsNonpositiveQuantity(long quantity) {
        assertThrows(IllegalArgumentException.class,
                () -> new OrderItem(1L, "Camisa", ClothingSize.S, quantity, BigDecimal.ONE));
    }
    @ParameterizedTest @ValueSource(strings={"-0.01", "100000000.00"})
    void rejectsNegativeOrOversizedPrice(String price) {
        assertThrows(IllegalArgumentException.class,
                () -> new OrderItem(1L, "Camisa", ClothingSize.S, 1, new BigDecimal(price)));
    }
    @Test void rejectsMissingPriceOrSizeAndFractionalCents() {
        assertThrows(NullPointerException.class, () -> new OrderItem(1L, "Camisa", null, 1, BigDecimal.ONE));
        assertThrows(NullPointerException.class, () -> new OrderItem(1L, "Camisa", ClothingSize.S, 1, null));
        assertThrows(ArithmeticException.class,
                () -> new OrderItem(1L, "Camisa", ClothingSize.S, 1, new BigDecimal("1.001")));
        var free = new OrderItem(1L, "Regalo", ClothingSize.TU, 1, BigDecimal.ZERO);
        assertEquals(new BigDecimal("0.00"), free.getLineTotal());
    }
    @Test void orderRequiresCustomerAddressAndItems() {
        assertThrows(NullPointerException.class, () -> new Order(null, address, List.of(item()), BigDecimal.ZERO));
        assertThrows(NullPointerException.class, () -> new Order(customer, null, List.of(item()), BigDecimal.ZERO));
        assertThrows(NullPointerException.class, () -> new Order(customer, address, null, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new Order(customer, address, List.of(), BigDecimal.ZERO));
    }
    @Test void rejectsInvalidShippingAmounts() {
        assertThrows(NullPointerException.class, () -> new Order(customer, address, List.of(item()), null));
        assertThrows(IllegalArgumentException.class,
                () -> new Order(customer, address, List.of(item()), new BigDecimal("-0.01")));
        assertThrows(IllegalArgumentException.class,
                () -> new Order(customer, address, List.of(item()), new BigDecimal("100000000000000000.00")));
        assertThrows(ArithmeticException.class,
                () -> new Order(customer, address, List.of(item()), new BigDecimal("0.001")));
    }
    @Test void copiesOrderItemsAndExposesAnImmutableSnapshot() {
        var items = new ArrayList<>(List.of(item()));
        var order = new Order(customer, address, items, new BigDecimal("3.95"));
        items.clear();
        assertEquals(1, order.getItems().size());
        assertThrows(UnsupportedOperationException.class, () -> order.getItems().clear());
        assertEquals(new BigDecimal("80.00"), order.getSubtotal());
        assertEquals(new BigDecimal("83.95"), order.getTotal());
    }
    @Test void cannotPayExpiredOrExpirePaidOrder() {
        var expired = order();
        expired.markExpired();
        expired.markExpired();
        assertThrows(IllegalStateException.class, expired::markPaid);
        var paid = order();
        paid.markPaid();
        paid.markPaid();
        assertThrows(IllegalStateException.class, paid::markExpired);
    }
    @Test void fulfillmentOnlyAllowsSequentialTransitionsAndRepeatingCurrentState() {
        var order = order();
        assertThrows(NullPointerException.class, () -> order.advanceFulfillment(null));
        assertThrows(IllegalArgumentException.class, () -> order.advanceFulfillment(OrderStatus.PAID));
        assertThrows(IllegalStateException.class, () -> order.advanceFulfillment(OrderStatus.PREPARING));
        order.markPaid();
        assertThrows(IllegalStateException.class, () -> order.advanceFulfillment(OrderStatus.SHIPPED));
        order.advanceFulfillment(OrderStatus.PREPARING);
        order.advanceFulfillment(OrderStatus.PREPARING);
        assertThrows(IllegalStateException.class, () -> order.advanceFulfillment(OrderStatus.DELIVERED));
        order.advanceFulfillment(OrderStatus.SHIPPED);
        assertThrows(IllegalStateException.class, () -> order.advanceFulfillment(OrderStatus.PREPARING));
        order.advanceFulfillment(OrderStatus.DELIVERED);
        assertThrows(IllegalStateException.class, () -> order.advanceFulfillment(OrderStatus.SHIPPED));
    }
    @Test void paymentRequiresAnUnpaidOrder() {
        assertThrows(NullPointerException.class, () -> new OrderPayment(null));
        var paid = order(); paid.markPaid();
        assertThrows(IllegalArgumentException.class, () -> new OrderPayment(paid));
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={"cs_live_example", "invalid"})
    void onlyAllowsTestCheckoutSessions(String session) {
        assertThrows(IllegalArgumentException.class, () -> new OrderPayment(order()).attachSession(session, Instant.now()));
    }
    @Test void sessionAssociationIsImmutableAndIdempotent() {
        var payment = new OrderPayment(order());
        var expiry = Instant.parse("2027-01-01T00:00:00Z");
        assertThrows(IllegalArgumentException.class, () -> payment.attachSession("cs_test_" + "a".repeat(250), expiry));
        assertThrows(NullPointerException.class, () -> payment.attachSession("cs_test_a", null));
        payment.attachSession("cs_test_a", expiry);
        payment.attachSession("cs_test_a", expiry);
        assertThrows(IllegalStateException.class, () -> payment.attachSession("cs_test_b", expiry));
        assertThrows(IllegalStateException.class, () -> payment.attachSession("cs_test_a", expiry.plusSeconds(1)));
        assertEquals("cs_test_a", payment.getStripeSessionId());
        assertEquals(expiry, payment.getSessionExpiresAt());
    }
    @Test void stockReservationCannotBeConsumedOrReleasedBeforeItExists() {
        var payment = new OrderPayment(order());
        assertThrows(IllegalStateException.class, payment::markStockConsumed);
        assertThrows(IllegalStateException.class, payment::markStockReleased);
        payment.markStockReserved();
        payment.markStockReserved();
        payment.markStockConsumed();
        assertThrows(IllegalStateException.class, payment::markStockReserved);
    }
}
