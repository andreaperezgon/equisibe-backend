package com.equisibe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ShippingAddressRequest(

        @NotBlank(message = "El nombre del destinatario es obligatorio.")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres.")
        String recipientName,

        @NotBlank(message = "El teléfono es obligatorio.")
        @Pattern(
                regexp = "^[+]?[0-9 ()-]{7,25}$",
                message = "Introduce un teléfono válido."
        )
        String phone,

        @NotBlank(message = "La dirección es obligatoria.")
        @Size(max = 200, message = "La dirección no puede superar 200 caracteres.")
        String addressLine1,

        @Size(max = 100, message = "Los datos adicionales no pueden superar 100 caracteres.")
        String addressLine2,

        @NotBlank(message = "La localidad es obligatoria.")
        @Size(max = 100, message = "La localidad no puede superar 100 caracteres.")
        String city,

        @Size(max = 100, message = "La provincia no puede superar 100 caracteres.")
        String province,

        @NotBlank(message = "El código postal es obligatorio.")
        @Size(max = 20, message = "El código postal no puede superar 20 caracteres.")
        String postalCode,

        @NotBlank(message = "El país es obligatorio.")
        @Pattern(
                regexp = "^[A-Z]{2}$",
                message = "El país debe indicarse con dos letras mayúsculas."
        )
        String countryCode

) {
}