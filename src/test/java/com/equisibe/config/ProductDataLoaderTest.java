package com.equisibe.config;

import com.equisibe.model.ClothingSize;
import com.equisibe.model.Product;
import com.equisibe.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductDataLoaderTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductDataLoader productDataLoader;

    @Captor
    private ArgumentCaptor<List<Product>> productsCaptor;

    @Test
    void shouldLoadSixProductsWhenCatalogIsEmpty() {
        List<Product> products = loadProducts();

        assertEquals(6, products.size());

        assertProduct(products, "Camisa Equisibé",
                "49.90", "CAMISA.jpeg", "Camisas");
        assertProduct(products, "Camiseta Equisibé",
                "29.90", "CAMISETA.jpeg", "Camisetas");
        assertProduct(products, "Gorra Equisibé",
                "24.90", "GORRA.jpeg", "Gorras");
        assertProduct(products, "Jersey Equisibé",
                "59.90", "JERSEY.png", "Jerséis");
        assertProduct(products, "Pantalón Equisibé",
                "69.90", "PANTALON.jpeg", "Pantalones");
        assertProduct(products, "Sudadera Equisibé",
                "54.90", "SUDADERA.jpeg", "Sudaderas");
    }

    @Test
    void shouldGiveCapOnlyOneSize() {
        Product cap = findProduct(loadProducts(), "Gorra Equisibé");

        assertEquals(
                Map.of(ClothingSize.TU, 10),
                cap.getStockBySize()
        );
    }

    @Test
    void shouldGiveClothingFourSizesWithSoldOutXL() {
        List<Product> clothing = loadProducts().stream()
                .filter(product -> !product.getName().equals("Gorra Equisibé"))
                .toList();

        assertEquals(5, clothing.size());

        Map<ClothingSize, Integer> expectedStock = Map.of(
                ClothingSize.S, 5,
                ClothingSize.M, 8,
                ClothingSize.L, 4,
                ClothingSize.XL, 0
        );

        for (Product product : clothing) {
            assertEquals(
                    expectedStock,
                    product.getStockBySize(),
                    product.getName()
            );
        }
    }

    @Test
    void shouldSkipLoadingWhenProductsAlreadyExist() {
        when(productRepository.count()).thenReturn(1L);

        productDataLoader.run();

        verify(productRepository).count();
        verifyNoMoreInteractions(productRepository);
    }

    private List<Product> loadProducts() {
        when(productRepository.count()).thenReturn(0L);

        productDataLoader.run();

        verify(productRepository).saveAll(productsCaptor.capture());

        return productsCaptor.getValue();
    }

    private Product findProduct(List<Product> products, String name) {
        return products.stream()
                .filter(product -> product.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No se encontró el producto: " + name
                ));
    }

    private void assertProduct(
            List<Product> products,
            String name,
            String price,
            String imageName,
            String category) {

        Product product = findProduct(products, name);

        assertEquals(new BigDecimal(price), product.getPrice());
        assertEquals(
                "/images/products/" + imageName,
                product.getImageUrl()
        );
        assertEquals(category, product.getCategory());
        assertTrue(product.isActive());
        assertNotNull(product.getDescription());
        assertFalse(product.getDescription().isBlank());
    }
}