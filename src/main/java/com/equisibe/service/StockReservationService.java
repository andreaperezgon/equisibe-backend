package com.equisibe.service;

import com.equisibe.model.*;
import com.equisibe.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class StockReservationService {

    private final ProductRepository products;
    private final EntityManager entityManager;

    public StockReservationService(
            ProductRepository products,
            EntityManager entityManager) {
        this.products = products;
        this.entityManager = entityManager;
    }

    public void reserve(OrderPayment payment) {
        entityManager.lock(payment.getOrder(), LockModeType.PESSIMISTIC_WRITE);

        if (payment.getStockReservationStatus() == StockReservationStatus.RESERVED) {
            return;
        }
        if (payment.getStockReservationStatus() != StockReservationStatus.NONE) {
            throw conflict("La reserva del pedido ya no está disponible.");
        }

        var quantities = quantities(payment);
        var locked = lockProducts(quantities);

        for (var productEntry : quantities.entrySet()) {
            Product product = locked.get(productEntry.getKey());
            if (!product.isActive()) {
                throw conflict("Una de las prendas ya no está disponible.");
            }
            for (var size : productEntry.getValue().entrySet()) {
                if (product.getStock(size.getKey()) < size.getValue()) {
                    throw conflict(
                            "No quedan suficientes unidades de "
                            + product.getName() + ", talla " + size.getKey() + ".");
                }
            }
        }

        for (var productEntry : quantities.entrySet()) {
            Product product = locked.get(productEntry.getKey());
            productEntry.getValue().forEach((size, quantity) -> product.takeStock(size, quantity));
        }
        payment.markStockReserved();
        entityManager.flush();
    }

    public void consume(OrderPayment payment) {
        entityManager.lock(payment.getOrder(), LockModeType.PESSIMISTIC_WRITE);

        if (payment.getStockReservationStatus() == StockReservationStatus.CONSUMED) {
            return;
        }

        payment.markStockConsumed();
        entityManager.flush();
    }

    public void release(OrderPayment payment) {
        entityManager.lock(payment.getOrder(), LockModeType.PESSIMISTIC_WRITE);

        if (payment.getStockReservationStatus() == StockReservationStatus.RELEASED
                || payment.getStockReservationStatus() == StockReservationStatus.NONE) {
            return;
        }
        if (payment.getStockReservationStatus() == StockReservationStatus.CONSUMED) {
            throw conflict("No se pueden liberar unidades de un pago confirmado.");
        }

        var quantities = quantities(payment);
        var locked = lockProducts(quantities);

        for (var productEntry : quantities.entrySet()) {
            Product product = locked.get(productEntry.getKey());
            productEntry.getValue().forEach((size, quantity) -> product.restoreStock(size, quantity));
        }
        payment.markStockReleased();
        entityManager.flush();
    }

    private Map<Long, Map<ClothingSize, Integer>> quantities(OrderPayment payment) {
        Map<Long, Map<ClothingSize, Integer>> result = new TreeMap<>();

        for (var item : payment.getOrder().getItems()) {
            if (item.getQuantity() <= 0 || item.getQuantity() > Integer.MAX_VALUE) {
                throw conflict("La cantidad del pedido no es válida.");
            }
            var sizes = result.computeIfAbsent(
                    item.getProductId(), ignored -> new EnumMap<>(ClothingSize.class));
            sizes.merge(item.getSize(), Math.toIntExact(item.getQuantity()), (previous, quantity) -> Math.addExact(previous, quantity));
        }
        return result;
    }

    private Map<Long, Product> lockProducts(
            Map<Long, Map<ClothingSize, Integer>> quantities) {
        Map<Long, Product> locked = new TreeMap<>();

        // Siempre en el mismo orden para reducir bloqueos entre compras.
        for (Long productId : quantities.keySet()) {
            Product product = products.findLockedById(productId)
                    .orElseThrow(() -> conflict("Una prenda ya no está disponible."));
            entityManager.refresh(product, LockModeType.PESSIMISTIC_WRITE);
            locked.put(productId, product);
        }
        return locked;
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
