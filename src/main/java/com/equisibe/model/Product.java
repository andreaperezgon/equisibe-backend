package com.equisibe.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 1000)
    private String imageUrl;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false)
    private boolean active = true;

    @ElementCollection
    @CollectionTable(
            name = "product_size_stock",
            joinColumns = @JoinColumn(name = "product_id")
    )
    @MapKeyColumn(name = "size", length = 2)
    @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "quantity", nullable = false)
    private Map<ClothingSize, Integer> stockBySize =
            new EnumMap<>(ClothingSize.class);

    protected Product() {
    }

    public Product(
            String name,
            String description,
            BigDecimal price,
            String imageUrl,
            String category) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    public void updateStock(ClothingSize size, int quantity) {
        Objects.requireNonNull(size, "La talla es obligatoria.");

        if (quantity < 0) {
            throw new IllegalArgumentException(
                    "El stock no puede ser negativo."
            );
        }

        stockBySize.put(size, quantity);
    }

    public int getStock(ClothingSize size) {
        Objects.requireNonNull(size, "La talla es obligatoria.");
        return stockBySize.getOrDefault(size, 0);
    }

    public Map<ClothingSize, Integer> getStockBySize() {
        var stock = new EnumMap<ClothingSize, Integer>(
                ClothingSize.class
        );

        if (stockBySize.containsKey(ClothingSize.TU)) {
            stock.put(ClothingSize.TU, getStock(ClothingSize.TU));
        } else {
            stock.put(ClothingSize.S, getStock(ClothingSize.S));
            stock.put(ClothingSize.M, getStock(ClothingSize.M));
            stock.put(ClothingSize.L, getStock(ClothingSize.L));
            stock.put(ClothingSize.XL, getStock(ClothingSize.XL));
        }

        return Collections.unmodifiableMap(stock);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getCategory() {
        return category;
    }

    public boolean isActive() {
        return active;
    }
}