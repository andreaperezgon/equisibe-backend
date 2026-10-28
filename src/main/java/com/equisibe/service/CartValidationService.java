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
    private final ShippingCostService shippingCostService;

    public CartValidationService(
            ProductRepository productRepository,
            ShippingCostService shippingCostService) {

        this.productRepository = productRepository;
        this.shippingCostService = shippingCostService;
    }

    public CartValidationResponse validate(CartValidationRequest request) {
        Map<LineKey, Long> quantities = new LinkedHashMap<>();

        for (var item : request.items()) {
            var key = new LineKey(item.productId(), item.size());

            quantities.merge(
                    key,
                    item.quantity().longValue(),
                    (previous, quantity) -> previous + quantity
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

            var lineTotal = product.getEffectivePrice()
                    .multiply(BigDecimal.valueOf(quantity));

            subtotal = subtotal.add(lineTotal);

            items.add(new CartValidationResponse.Item(
                    product.getId(),
                    product.getName(),
                    key.size(),
                    quantity,
                    product.getEffectivePrice(),
                    lineTotal
            ));
        }

        boolean valid = issues.isEmpty();

        BigDecimal shippingCost = valid
                ? shippingCostService.calculate(subtotal)
                : null;

        BigDecimal total = valid
                ? subtotal.add(shippingCost)
                : null;

        return new CartValidationResponse(
                valid,
                List.copyOf(items),
                List.copyOf(issues),
                valid ? subtotal : null,
                shippingCost,
                total,
                "EUR"
        );
    }

    private record LineKey(Long productId, ClothingSize size) {
    }
}