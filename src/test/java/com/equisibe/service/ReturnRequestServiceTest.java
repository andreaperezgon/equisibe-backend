package com.equisibe.service;

import com.equisibe.dto.ReturnRequestInput;
import com.equisibe.model.*;
import com.equisibe.repository.ReturnRequestRepository;
import com.equisibe.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReturnRequestServiceTest {

    @Mock ReturnRequestRepository requests;
    @Mock UserRepository users;
    @Mock EntityManager entityManager;
    @InjectMocks ReturnRequestService service;

    private final String email = "cliente@example.com";

    private ReturnRequestInput input(UUID id, long quantity) {
        return new ReturnRequestInput(
                id, ReturnType.WITHDRAWAL, "",
                List.of(new ReturnRequestInput.Item(0, quantity)));
    }

    private Order ownedOrder() {
        User user = mock(User.class);
        Order order = mock(Order.class);

        when(users.findByEmail(email)).thenReturn(Optional.of(user));
        when(user.isEnabled()).thenReturn(true);
        when(user.getId()).thenReturn(1L);
        when(order.getUser()).thenReturn(user);
        when(entityManager.find(Order.class, 10L, LockModeType.PESSIMISTIC_WRITE))
                .thenReturn(order);

        return order;
    }

    private List<OrderItem> purchasedItems() {
        return List.of(new OrderItem(
                5L, "Camisa", ClothingSize.M, 2, new BigDecimal("40.00")));
    }

    @Test
    void shouldCreateReturnForOwnedPaidOrder() {
        Order order = ownedOrder();
        UUID id = UUID.randomUUID();

        when(order.getId()).thenReturn(10L);
        when(order.getStatus()).thenReturn(OrderStatus.PAID);
        when(order.getItems()).thenReturn(purchasedItems());
        when(requests.findByClientRequestId(id)).thenReturn(Optional.empty());
        when(requests.findByOrder_IdOrderByCreatedAtDescIdDesc(10L))
                .thenReturn(List.of());
        when(requests.saveAndFlush(any(ReturnRequest.class)))
                .thenAnswer(call -> call.getArgument(0));

        var result = service.create(email, 10L, input(id, 1));

        assertEquals(10L, result.orderId());
        assertEquals(ReturnStatus.REQUESTED, result.status());
        assertEquals(1, result.items().get(0).quantity());
    }

    @Test
    void shouldHideOrdersBelongingToAnotherCustomer() {
        User customer = mock(User.class);
        User owner = mock(User.class);
        Order order = mock(Order.class);

        when(users.findByEmail(email)).thenReturn(Optional.of(customer));
        when(customer.isEnabled()).thenReturn(true);
        when(customer.getId()).thenReturn(1L);
        when(owner.getId()).thenReturn(2L);
        when(order.getUser()).thenReturn(owner);
        when(entityManager.find(Order.class, 10L)).thenReturn(order);

        var error = assertThrows(ResponseStatusException.class,
                () -> service.list(email, 10L));

        assertEquals(404, error.getStatusCode().value());
        verifyNoInteractions(requests);
    }

    @Test
    void shouldRejectUnpaidOrder() {
        Order order = ownedOrder();
        UUID id = UUID.randomUUID();

        when(requests.findByClientRequestId(id)).thenReturn(Optional.empty());
        when(order.getStatus()).thenReturn(OrderStatus.PENDING_PAYMENT);

        var error = assertThrows(ResponseStatusException.class,
                () -> service.create(email, 10L, input(id, 1)));

        assertEquals(409, error.getStatusCode().value());
        verify(requests, never()).saveAndFlush(any());
    }

    @Test
    void shouldCountUnitsAlreadyRequested() {
        Order order = ownedOrder();
        UUID id = UUID.randomUUID();
        ReturnRequest previous = new ReturnRequest(
                UUID.randomUUID(), order, ReturnType.WITHDRAWAL, "",
                List.of(new ReturnItem(0, 1)));

        when(order.getStatus()).thenReturn(OrderStatus.DELIVERED);
        when(order.getItems()).thenReturn(purchasedItems());
        when(requests.findByClientRequestId(id)).thenReturn(Optional.empty());
        when(requests.findByOrder_IdOrderByCreatedAtDescIdDesc(10L))
                .thenReturn(List.of(previous));

        var error = assertThrows(ResponseStatusException.class,
                () -> service.create(email, 10L, input(id, 2)));

        assertEquals(409, error.getStatusCode().value());
        verify(requests, never()).saveAndFlush(any());
    }

    @Test
    void shouldNotDuplicateSameRequest() {
        Order order = ownedOrder();
        UUID id = UUID.randomUUID();
        ReturnRequest previous = new ReturnRequest(
                id, order, ReturnType.WITHDRAWAL, "",
                List.of(new ReturnItem(0, 1)));

        when(order.getId()).thenReturn(10L);
        when(order.getItems()).thenReturn(purchasedItems());
        when(requests.findByClientRequestId(id))
                .thenReturn(Optional.of(previous));

        var result = service.create(email, 10L, input(id, 1));

        assertEquals(10L, result.orderId());
        verify(requests, never()).saveAndFlush(any());
    }

    @Test
    void shouldRejectSizeExchangeRequest() {
        var input = new ReturnRequestInput(
                UUID.randomUUID(), ReturnType.EXCHANGE, "",
                List.of(new ReturnRequestInput.Item(0, 1L)));

        var error = assertThrows(ResponseStatusException.class,
                () -> service.create(email, 10L, input));

        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(users, requests, entityManager);
    }

    @Test
    void shouldRequireExplanationToRejectReturn() {
        ReturnRequest request = new ReturnRequest(
                UUID.randomUUID(), mock(Order.class), ReturnType.WITHDRAWAL, "",
                List.of(new ReturnItem(0, 1)));

        assertThrows(IllegalArgumentException.class,
                () -> request.updateStatus(ReturnStatus.REJECTED, ""));

        request.updateStatus(ReturnStatus.REJECTED, "Motivo explicado al cliente.");
        assertEquals(ReturnStatus.REJECTED, request.getStatus());
        assertEquals("Motivo explicado al cliente.", request.getAdminNote());
    }
}
