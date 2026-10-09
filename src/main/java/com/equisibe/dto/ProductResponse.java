package com.equisibe.dto;

import com.equisibe.model.ClothingSize;
import java.math.BigDecimal;
import java.util.Map;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        String category,
        Map<ClothingSize, Integer> stockBySize
) {
}