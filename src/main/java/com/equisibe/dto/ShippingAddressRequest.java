package com.equisibe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ShippingAddressRequest(

        @NotBlank(message = "El nombre del destinatario es obligatorio.")
        @Size(max = 100)
        String recipientName,

        @NotBlank(message = "El teléfono es obligatorio.")
        @Pattern(
                regexp = "^[+]?[0-9 ()-]{7,25}$",
                message = "Introduce un teléfono válido."
        )
        String phone,

        @NotBlank(message = "La dirección es obligatoria.")
        @Size(max = 200)
        String addressLine1,

        @Size(max = 100)
        String addressLine2,

        @NotBlank(message = "La localidad es obligatoria.")
        @Size(max = 100)
        String city,

        @Size(max = 100)
        String province,

        @NotBlank(message = "El código postal es obligatorio.")
        @Pattern(
                regexp = "^[0-9]{5}$",
                message = "El código postal debe tener cinco cifras."
        )
        String postalCode,

        @NotBlank(message = "El país es obligatorio.")
        @Pattern(
                regexp = "^ES$",
                message = "Solo realizamos envíos a España."
        )
        String countryCode

) {
}