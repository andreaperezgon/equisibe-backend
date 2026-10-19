package com.equisibe.dto;

import com.equisibe.model.ClothingSize;
import com.equisibe.model.Product;
import java.math.BigDecimal;
import java.util.Map;

public record AdminProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        BigDecimal salePrice,
        String imageUrl,
        String category,
        boolean active,
        Map<ClothingSize, Integer> stockBySize
) {

    public static AdminProductResponse from(Product product) {
        return new AdminProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getSalePrice(),
                product.getImageUrl(),
                product.getCategory(),
                product.isActive(),
                product.getStockBySize()
        );
    }
}
