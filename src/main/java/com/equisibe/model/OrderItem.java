package com.equisibe.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Embeddable
public class OrderItem {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(name = "size", nullable = false, length = 2)
    private ClothingSize size;

    @Column(name = "quantity", nullable = false)
    private long quantity;

    @Column(
            name = "unit_price",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal unitPrice;

    protected OrderItem() {
    }

    public OrderItem(
            Long productId,
            String productName,
            ClothingSize size,
            long quantity,
            BigDecimal unitPrice) {

        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del producto debe ser positivo."
            );
        }

        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre del producto es obligatorio."
            );
        }

        String normalizedName = productName.trim();

        if (normalizedName.length() > 100) {
            throw new IllegalArgumentException(
                    "El nombre del producto no puede superar 100 caracteres."
            );
        }

        Objects.requireNonNull(size, "La talla es obligatoria.");
        Objects.requireNonNull(
                unitPrice,
                "El precio unitario es obligatorio."
        );

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad debe ser mayor que cero."
            );
        }

        if (unitPrice.signum() < 0) {
            throw new IllegalArgumentException(
                    "El precio no puede ser negativo."
            );
        }

        BigDecimal normalizedPrice = unitPrice.setScale(
                2,
                RoundingMode.UNNECESSARY
        );

        if (normalizedPrice.precision() > 10) {
            throw new IllegalArgumentException(
                    "El precio supera el importe máximo admitido."
            );
        }

        this.productId = productId;
        this.productName = normalizedName;
        this.size = size;
        this.quantity = quantity;
        this.unitPrice = normalizedPrice;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public ClothingSize getSize() {
        return size;
    }

    public long getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}