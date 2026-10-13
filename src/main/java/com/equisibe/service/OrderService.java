package com.equisibe.service;

import com.equisibe.dto.CheckoutRequest;
import com.equisibe.dto.CheckoutResponse;
import com.equisibe.dto.ShippingAddressResponse;
import com.equisibe.model.Order;
import com.equisibe.model.OrderItem;
import com.equisibe.model.OrderShippingAddress;
import com.equisibe.model.User;
import com.equisibe.repository.OrderRepository;
import com.equisibe.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CheckoutService checkoutService;

    public OrderService(
            OrderRepository orderRepository,
            UserRepository userRepository,
            CheckoutService checkoutService) {

        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.checkoutService = checkoutService;
    }

    @Transactional
    public Long createPendingOrder(
            String email,
            CheckoutRequest request) {

        User user = getActiveUser(email);

        CheckoutResponse checkout = checkoutService.validate(
                email,
                request
        );

        var cart = checkout.cart();

        if (!cart.valid() || cart.items().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Revisa el carrito antes de crear el pedido."
            );
        }

        var items = cart.items().stream()
                .map(item -> new OrderItem(
                        item.productId(),
                        item.name(),
                        item.size(),
                        item.quantity(),
                        item.unitPrice()
                ))
                .toList();

        Order order = new Order(
                user,
                copyAddress(checkout.shippingAddress()),
                items,
                cart.shippingCost()
        );

        return orderRepository.save(order).getId();
    }

    private User getActiveUser(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Debes iniciar sesión."
            );
        }

        return userRepository.findByEmail(email)
                .filter(User::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "La sesión no es válida."
                ));
    }

    private OrderShippingAddress copyAddress(
            ShippingAddressResponse address) {

        return new OrderShippingAddress(
                address.recipientName(),
                address.phone(),
                address.addressLine1(),
                address.addressLine2(),
                address.city(),
                address.province(),
                address.postalCode(),
                address.countryCode()
        );
    }
}