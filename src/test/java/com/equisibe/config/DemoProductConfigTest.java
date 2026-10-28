package com.equisibe.config;

import com.equisibe.model.ClothingSize;
import com.equisibe.model.Product;
import com.equisibe.repository.ProductRepository;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DemoProductConfigTest {
    @Test void doesNotOverwriteAnExistingCatalog() throws Exception {
        var repository = mock(ProductRepository.class);
        when(repository.count()).thenReturn(1L);
        new DemoProductConfig().loadDemoProducts(repository).run();
        verify(repository, never()).saveAll(any());
    }

    @Test void seedsClothingAndOneSizeCapWhenCatalogIsEmpty() throws Exception {
        var repository = mock(ProductRepository.class);
        var saved = new ArrayList<Product>();
        when(repository.saveAll(any())).thenAnswer(invocation -> {
            Iterable<Product> products = invocation.getArgument(0);
            products.forEach(saved::add);
            return saved;
        });
        new DemoProductConfig().loadDemoProducts(repository).run();
        assertEquals(6, saved.size());
        assertEquals(6, saved.stream().map(product -> product.getName()).distinct().count());
        for (var product : saved.subList(0, 5)) {
            assertEquals(3, product.getStockBySize().get(ClothingSize.S));
            assertEquals(0, product.getStockBySize().get(ClothingSize.M));
            assertEquals(4, product.getStockBySize().get(ClothingSize.L));
            assertEquals(2, product.getStockBySize().get(ClothingSize.XL));
            assertTrue(product.getImageUrl().startsWith("/images/products/"));
            assertTrue(product.getPrice().signum() > 0);
        }
        assertEquals(java.util.Map.of(ClothingSize.TU, 5), saved.get(5).getStockBySize());
    }
}
