package com.equisibe.dto;

import com.equisibe.model.ReturnType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public record ReturnRequestInput(
        @NotNull UUID clientRequestId,
        @NotNull ReturnType type,
        @Size(max = 2000) String note,
        @NotEmpty @Size(max = 100)
        List<@NotNull @Valid Item> items
) {
    public record Item(
            @NotNull @Min(0) @Max(99) Integer lineIndex,
            @NotNull @Positive @Max(2147483647L) Long quantity
    ) {
    }
}
