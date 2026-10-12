package com.equisibe.dto;

import com.equisibe.model.ClothingSize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CartValidationRequest(
        @NotNull
        @Size(min = 1, max = 100)
        List<@NotNull @Valid Item> items
) {

    public record Item(
            @NotNull @Positive Long productId,
            @NotNull ClothingSize size,
            @NotNull @Positive Integer quantity
    ) {
    }
}