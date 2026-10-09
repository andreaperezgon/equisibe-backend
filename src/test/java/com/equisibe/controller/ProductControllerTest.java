package com.equisibe.controller;

import com.equisibe.dto.ProductResponse;
import com.equisibe.model.ClothingSize;
import com.equisibe.service.ProductService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProductController(productService))
                .build();
    }

    @Test
    void shouldReturnProductCatalogWithSizeStock() throws Exception {
        when(productService.getProducts())
                .thenReturn(List.of(sampleProduct()));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Camisa blanca"))
                .andExpect(jsonPath("$[0].price").value(49.90))
                .andExpect(jsonPath("$[0].category").value("Camisas"))
                .andExpect(jsonPath("$[0].stockBySize.S").value(3))
                .andExpect(jsonPath("$[0].stockBySize.M").value(0))
                .andExpect(jsonPath("$[0].stockBySize.L").value(2))
                .andExpect(jsonPath("$[0].stockBySize.XL").value(0));

        verify(productService).getProducts();
    }

    @Test
    void shouldReturnEmptyCatalog() throws Exception {
        when(productService.getProducts()).thenReturn(List.of());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void shouldReturnProductDetailsWithSizeStock() throws Exception {
        when(productService.getProduct(1L)).thenReturn(sampleProduct());

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Camisa blanca"))
                .andExpect(jsonPath("$.description")
                        .value("Camisa de algodón."))
                .andExpect(jsonPath("$.imageUrl")
                        .value("/images/white-shirt.jpg"))
                .andExpect(jsonPath("$.stockBySize.S").value(3))
                .andExpect(jsonPath("$.stockBySize.M").value(0))
                .andExpect(jsonPath("$.stockBySize.L").value(2))
                .andExpect(jsonPath("$.stockBySize.XL").value(0));

        verify(productService).getProduct(1L);
    }

    @Test
    void shouldReturnNotFoundForUnavailableProduct() throws Exception {
        when(productService.getProduct(99L))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "La prenda no está disponible."
                ));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectInvalidProductId() throws Exception {
        mockMvc.perform(get("/api/products/invalid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    private ProductResponse sampleProduct() {
        return new ProductResponse(
                1L,
                "Camisa blanca",
                "Camisa de algodón.",
                new BigDecimal("49.90"),
                "/images/white-shirt.jpg",
                "Camisas",
                Map.of(
                        ClothingSize.S, 3,
                        ClothingSize.M, 0,
                        ClothingSize.L, 2,
                        ClothingSize.XL, 0
                )
        );
    }
}