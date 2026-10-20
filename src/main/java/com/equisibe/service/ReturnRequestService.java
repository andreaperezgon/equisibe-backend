package com.equisibe.service;

import com.equisibe.dto.ReturnRequestInput;
import com.equisibe.dto.ReturnResponse;
import com.equisibe.model.*;
import com.equisibe.repository.ReturnRequestRepository;
import com.equisibe.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ReturnRequestService {
    private final ReturnRequestRepository requests;
    private final UserRepository users;
    private final EntityManager entityManager;

    public ReturnRequestService(
            ReturnRequestRepository requests,
            UserRepository users,
            EntityManager entityManager) {
        this.requests = requests;
        this.users = users;
        this.entityManager = entityManager;
    }

    public List<ReturnResponse> list(String email, Long orderId) {
        ownedOrder(email, orderId, false);
        return requests.findByOrder_IdOrderByCreatedAtDescIdDesc(orderId)
                .stream().map(ReturnResponse::from).toList();
    }

    @Transactional
    public ReturnResponse create(
            String email, Long orderId, ReturnRequestInput input) {

        if (input.type() != ReturnType.WITHDRAWAL) {
            throw badRequest("Solo se admiten solicitudes de devolución.");
        }
        Order order = ownedOrder(email, orderId, true);

        var existing = requests.findByClientRequestId(input.clientRequestId());
        if (existing.isPresent()) {
            var previous = existing.get();
            if (!previous.getOrder().getId().equals(orderId)
                    || !sameRequest(previous, input)) {
                throw conflict("La solicitud ya existe con otros datos.");
            }
            return ReturnResponse.from(previous);
        }

        if (!Set.of(
                OrderStatus.PAID, OrderStatus.PREPARING,
                OrderStatus.SHIPPED, OrderStatus.DELIVERED
        ).contains(order.getStatus())) {
            throw conflict("El pedido debe tener el pago confirmado.");
        }

        Map<Integer, Long> quantities = new HashMap<>();
        for (var item : input.items()) {
            if (item.lineIndex() >= order.getItems().size()
                    || quantities.putIfAbsent(
                            item.lineIndex(), item.quantity()) != null) {
                throw badRequest("La selección de prendas no es válida.");
            }
        }

        Map<Integer, Long> previouslyRequested = new HashMap<>();
        for (var previous :
                requests.findByOrder_IdOrderByCreatedAtDescIdDesc(orderId)) {
            if (previous.getStatus() == ReturnStatus.REJECTED) continue;
            for (var item : previous.getItems()) {
                previouslyRequested.merge(
                        item.getLineIndex(), item.getQuantity(), Long::sum
                );
            }
        }

        for (var entry : quantities.entrySet()) {
            long purchased = order.getItems()
                    .get(entry.getKey()).getQuantity();
            long used = previouslyRequested.getOrDefault(entry.getKey(), 0L);
            if (entry.getValue() > purchased - used) {
                throw conflict(
                        "La cantidad supera las unidades disponibles para solicitar."
                );
            }
        }

        var items = input.items().stream()
                .map(item -> new ReturnItem(
                        item.lineIndex(), item.quantity()
                )).toList();

        var request = new ReturnRequest(
                input.clientRequestId(), order,
                input.type(), input.note(), items
        );

        return ReturnResponse.from(requests.saveAndFlush(request));
    }

    private boolean sameRequest(
            ReturnRequest previous, ReturnRequestInput input) {
        String note = input.note() == null ? "" : input.note().trim();
        if (previous.getType() != input.type()
                || !previous.getNote().equals(note)
                || previous.getItems().size() != input.items().size()) {
            return false;
        }
        Map<Integer, Long> quantities = new HashMap<>();
        for (var item : previous.getItems()) {
            quantities.put(item.getLineIndex(), item.getQuantity());
        }
        var seen = new java.util.HashSet<Integer>();
        return input.items().stream().allMatch(item ->
                seen.add(item.lineIndex())
                && java.util.Objects.equals(
                        quantities.get(item.lineIndex()), item.quantity()
                ));
    }

    private Order ownedOrder(String email, Long orderId, boolean lock) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        var user = users.findByEmail(email)
                .filter(item -> item.isEnabled())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (orderId == null || orderId <= 0) {
            throw badRequest("El pedido no es válido.");
        }

        Order order = lock
                ? entityManager.find(
                        Order.class, orderId, LockModeType.PESSIMISTIC_WRITE)
                : entityManager.find(Order.class, orderId);

        if (order == null || !order.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "El pedido no está disponible."
            );
        }
        return order;
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
