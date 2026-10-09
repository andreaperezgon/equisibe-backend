package com.equisibe.repository;

import com.equisibe.model.ClothingSize;
import com.equisibe.model.Product;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistStockForEachSize() {
        var product = new Product(
                "Camisa blanca",
                "Camisa de algodón.",
                new BigDecimal("49.90"),
                "/images/products/CAMISA.jpeg",
                "Camisas"
        );

        product.updateStock(ClothingSize.S, 3);
        product.updateStock(ClothingSize.M, 0);
        product.updateStock(ClothingSize.L, 2);
        product.updateStock(ClothingSize.XL, 1);

        var savedProduct = productRepository.saveAndFlush(product);
        Long id = savedProduct.getId();

        entityManager.clear();

        var loadedProduct = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow();

        assertEquals(
                Map.of(
                        ClothingSize.S, 3,
                        ClothingSize.M, 0,
                        ClothingSize.L, 2,
                        ClothingSize.XL, 1
                ),
                loadedProduct.getStockBySize()
        );
    }

    @Test
    void shouldReturnZeroForClothingSizesWithoutStock() {
        var product = new Product(
                "Camisa blanca",
                "Camisa de algodón.",
                new BigDecimal("49.90"),
                "/images/products/CAMISA.jpeg",
                "Camisas"
        );

        var savedProduct = productRepository.saveAndFlush(product);
        Long id = savedProduct.getId();

        entityManager.clear();

        var loadedProduct = productRepository.findById(id).orElseThrow();

        assertEquals(
                Map.of(
                        ClothingSize.S, 0,
                        ClothingSize.M, 0,
                        ClothingSize.L, 0,
                        ClothingSize.XL, 0
                ),
                loadedProduct.getStockBySize()
        );

        for (ClothingSize size : ClothingSize.values()) {
            assertEquals(0, loadedProduct.getStock(size));
        }
    }

    @Test
    void shouldPersistOnlyOneSizeForCap() {
        var product = new Product(
                "Gorra Equisibé",
                "Gorra de talla única.",
                new BigDecimal("24.90"),
                "/images/products/GORRA.jpeg",
                "Gorras"
        );

        product.updateStock(ClothingSize.TU, 10);

        var savedProduct = productRepository.saveAndFlush(product);
        Long id = savedProduct.getId();

        entityManager.clear();

        var loadedProduct = productRepository.findById(id).orElseThrow();

        assertEquals(
                Map.of(ClothingSize.TU, 10),
                loadedProduct.getStockBySize()
        );
    }

    @Test
    void shouldKeepOneSizeWhenCapIsSoldOut() {
        var product = new Product(
                "Gorra Equisibé",
                "Gorra de talla única.",
                new BigDecimal("24.90"),
                "/images/products/GORRA.jpeg",
                "Gorras"
        );

        product.updateStock(ClothingSize.TU, 0);

        var savedProduct = productRepository.saveAndFlush(product);
        Long id = savedProduct.getId();

        entityManager.clear();

        var loadedProduct = productRepository.findById(id).orElseThrow();

        assertEquals(
                Map.of(ClothingSize.TU, 0),
                loadedProduct.getStockBySize()
        );
    }
}