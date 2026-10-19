package com.equisibe.service;

import com.equisibe.dto.AdminOrderResponse;
import com.equisibe.model.Order;
import com.equisibe.model.OrderStatus;
import com.equisibe.model.Role;
import com.equisibe.repository.OrderRepository;
import com.equisibe.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class AdminOrderService {

    private static final Set<OrderStatus> PAID_STATES = Set.of(
            OrderStatus.PAID,
            OrderStatus.PREPARING,
            OrderStatus.SHIPPED,
            OrderStatus.DELIVERED
    );

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    public AdminOrderService(
            OrderRepository orderRepository,
            UserRepository userRepository,
            EntityManager entityManager) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.entityManager = entityManager;
    }

    public List<AdminOrderResponse> getOrders(String email) {
        requireAdmin(email);

        return orderRepository
                .findByStatusInOrderByCreatedAtDescIdDesc(PAID_STATES)
                .stream()
                .map(AdminOrderResponse::from)
                .toList();
    }

    public AdminOrderResponse getOrder(String email, Long orderId) {
        requireAdmin(email);
        validateId(orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> notFound());

        return AdminOrderResponse.from(order);
    }

    @Transactional
    public AdminOrderResponse updateStatus(
            String email,
            Long orderId,
            OrderStatus next) {
        requireAdmin(email);
        validateId(orderId);

        Order order = entityManager.find(
                Order.class,
                orderId,
                LockModeType.PESSIMISTIC_WRITE
        );

        if (order == null) {
            throw notFound();
        }

        try {
            order.advanceFulfillment(next);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    exception.getMessage()
            );
        }

        entityManager.flush();
        return AdminOrderResponse.from(order);
    }

    private void validateId(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador del pedido no es válido."
            );
        }
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "El pedido no existe."
        );
    }

    private void requireAdmin(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Debes iniciar sesión."
            );
        }

        userRepository.findByEmail(email)
                .filter(user -> user.isEnabled()
                        && user.getRole() == Role.ADMIN)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Necesitas permisos de administrador."
                ));
    }
}
