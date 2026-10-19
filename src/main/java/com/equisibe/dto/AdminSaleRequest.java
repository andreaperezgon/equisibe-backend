package com.equisibe.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

public record AdminSaleRequest(
        @DecimalMin("0.01")
        @Digits(integer = 8, fraction = 2)
        BigDecimal salePrice
) {
}
