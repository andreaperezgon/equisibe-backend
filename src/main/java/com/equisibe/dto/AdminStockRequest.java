package com.equisibe.dto;

import com.equisibe.model.ClothingSize;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.Map;

public record AdminStockRequest(
        @NotEmpty
        Map<@NotNull ClothingSize, @NotNull @PositiveOrZero Integer> stockBySize
) {
}
