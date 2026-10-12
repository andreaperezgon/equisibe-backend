package com.equisibe.dto;

import com.equisibe.dto.CartValidationRequest.Item;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CheckoutRequest(

        @NotNull(message = "Selecciona una dirección de envío.")
        @Positive(message = "La dirección de envío no es válida.")
        Long shippingAddressId,

        @NotNull(message = "La cesta es obligatoria.")
        @Size(
                min = 1,
                max = 100,
                message = "La cesta debe contener entre 1 y 100 líneas."
        )
        List<@NotNull @Valid Item> items

) {
}