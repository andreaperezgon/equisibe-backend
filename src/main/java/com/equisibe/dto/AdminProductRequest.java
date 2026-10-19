package com.equisibe.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AdminProductRequest(

        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 100, message = "El nombre admite hasta 100 caracteres.")
        String name,

        @NotBlank(message = "La descripción es obligatoria.")
        @Size(max = 2000, message = "La descripción es demasiado larga.")
        String description,

        @NotNull(message = "El precio es obligatorio.")
        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo.")
        @Digits(integer = 8, fraction = 2,
                message = "El precio admite hasta dos decimales.")
        BigDecimal price,

        @NotBlank(message = "La imagen es obligatoria.")
        @Size(max = 1000, message = "La dirección de imagen es demasiado larga.")
        @Pattern(
                regexp = "^(https://[^\\s]+|/(?!/)[^\\s]+)$",
                message = "Usa una dirección HTTPS o una ruta como /images/prenda.jpg."
        )
        String imageUrl,

        @NotBlank(message = "La categoría es obligatoria.")
        @Size(max = 100, message = "La categoría admite hasta 100 caracteres.")
        String category
) {
}
