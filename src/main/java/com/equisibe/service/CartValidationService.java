package com.equisibe.service;

import com.equisibe.dto.CartValidationRequest;
import com.equisibe.dto.CartValidationResponse;
import com.equisibe.model.ClothingSize;
import com.equisibe.model.Product;
import com.equisibe.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CartValidationService {

    private final ProductRepository productRepository;

    public CartValidationService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public CartValidationResponse validate(CartValidationRequest request) {
        Map<LineKey, Long> quantities = new LinkedHashMap<>();

        // Sumar las cantidades si se repiten el producto y la talla.
        for (var item : request.items()) {
            var key = new LineKey(item.productId(), item.size());

            quantities.merge(
                    key,
                    item.quantity().longValue(),
                    Long::sum
            );
        }

        Map<Long, Optional<Product>> products = new HashMap<>();
        var items = new ArrayList<CartValidationResponse.Item>();
        var issues = new ArrayList<CartValidationResponse.Issue>();
        var subtotal = new BigDecimal("0.00");

        for (var entry : quantities.entrySet()) {
            var key = entry.getKey();
            long quantity = entry.getValue();

            var found = products.computeIfAbsent(
                    key.productId(),
                    productRepository::findByIdAndActiveTrue
            );

            if (found.isEmpty()) {
                issues.add(new CartValidationResponse.Issue(
                        key.productId(),
                        key.size(),
                        "PRODUCT_UNAVAILABLE",
                        "La prenda ya no está disponible.",
                        null
                ));
                continue;
            }

            var product = found.get();
            var stockBySize = product.getStockBySize();

            if (!stockBySize.containsKey(key.size())) {
                issues.add(new CartValidationResponse.Issue(
                        key.productId(),
                        key.size(),
                        "INVALID_SIZE",
                        "La talla no corresponde a esta prenda.",
                        null
                ));
                continue;
            }

            int stock = stockBySize.get(key.size());

            if (quantity > stock) {
                issues.add(new CartValidationResponse.Issue(
                        key.productId(),
                        key.size(),
                        "INSUFFICIENT_STOCK",
                        "No hay suficientes unidades de esta talla.",
                        stock
                ));
                continue;
            }

            // Utilizar siempre el precio guardado en la base de datos.
            var lineTotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(quantity));

            subtotal = subtotal.add(lineTotal);

            items.add(new CartValidationResponse.Item(
                    product.getId(),
                    product.getName(),
                    key.size(),
                    quantity,
                    product.getPrice(),
                    lineTotal
            ));
        }

        boolean valid = issues.isEmpty();

        return new CartValidationResponse(
                valid,
                List.copyOf(items),
                List.copyOf(issues),
                valid ? subtotal : null,
                "EUR"
        );
    }

    private record LineKey(Long productId, ClothingSize size) {
    }
}