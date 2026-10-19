package com.equisibe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PopupRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 2000) String text,
        @Size(max = 40) String promoCode
) {
}
