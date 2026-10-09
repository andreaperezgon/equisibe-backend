package com.equisibe.service;

import com.equisibe.model.Product;
import com.equisibe.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void shouldReturnActiveProducts() {
        var product = new Product(
                "Camisa blanca",
                "Camisa de algodón.",
                new BigDecimal("49.90"),
                "/images/white-shirt.jpg",
                "Camisas"
        );

        when(productRepository.findByActiveTrueOrderByIdDesc())
                .thenReturn(List.of(product));

        var result = productService.getProducts();

        assertEquals(1, result.size());

        var response = result.getFirst();
        assertEquals("Camisa blanca", response.name());
        assertEquals("Camisa de algodón.", response.description());
        assertEquals(new BigDecimal("49.90"), response.price());
        assertEquals("/images/white-shirt.jpg", response.imageUrl());
        assertEquals("Camisas", response.category());

        verify(productRepository).findByActiveTrueOrderByIdDesc();
    }

    @Test
    void shouldReturnEmptyCatalogWhenNoProductsAreAvailable() {
        when(productRepository.findByActiveTrueOrderByIdDesc())
                .thenReturn(List.of());

        assertTrue(productService.getProducts().isEmpty());
    }

    @Test
    void shouldReturnAvailableProductById() {
        var product = new Product(
                "Camisa blanca",
                "Camisa de algodón.",
                new BigDecimal("49.90"),
                "/images/white-shirt.jpg",
                "Camisas"
        );

        when(productRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(product));

        var response = productService.getProduct(1L);

        assertEquals("Camisa blanca", response.name());
        assertEquals(new BigDecimal("49.90"), response.price());

        verify(productRepository).findByIdAndActiveTrue(1L);
    }

    @Test
    void shouldReturnNotFoundWhenProductIsUnavailable() {
        when(productRepository.findByIdAndActiveTrue(99L))
                .thenReturn(Optional.empty());

        var exception = assertThrows(
                ResponseStatusException.class,
                () -> productService.getProduct(99L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals(
                "La prenda no está disponible.",
                exception.getReason()
        );
    }
}