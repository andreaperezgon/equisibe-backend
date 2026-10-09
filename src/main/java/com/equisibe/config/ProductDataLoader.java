package com.equisibe.config;

import com.equisibe.model.ClothingSize;
import com.equisibe.model.Product;
import com.equisibe.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(
        name = "app.seed-products",
        havingValue = "true"
)
public class ProductDataLoader implements CommandLineRunner {

    private final ProductRepository productRepository;

    public ProductDataLoader(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (productRepository.count() > 0) {
            return;
        }

        Product camisa = createClothing(
                "Camisa Equisibé",
                "Camisa de la colección Equisibé.",
                "49.90",
                "CAMISA.jpeg",
                "Camisas"
        );

        Product camiseta = createClothing(
                "Camiseta Equisibé",
                "Camiseta de la colección Equisibé.",
                "29.90",
                "CAMISETA.jpeg",
                "Camisetas"
        );

        Product gorra = new Product(
                "Gorra Equisibé",
                "Gorra de talla única de la colección Equisibé.",
                new BigDecimal("24.90"),
                "/images/products/GORRA.jpeg",
                "Gorras"
        );
        gorra.updateStock(ClothingSize.TU, 10);

        Product jersey = createClothing(
                "Jersey Equisibé",
                "Jersey de la colección Equisibé.",
                "59.90",
                "JERSEY.png",
                "Jerséis"
        );

        Product pantalon = createClothing(
                "Pantalón Equisibé",
                "Pantalón de la colección Equisibé.",
                "69.90",
                "PANTALON.jpeg",
                "Pantalones"
        );

        Product sudadera = createClothing(
                "Sudadera Equisibé",
                "Sudadera de la colección Equisibé.",
                "54.90",
                "SUDADERA.jpeg",
                "Sudaderas"
        );

        productRepository.saveAll(List.of(
                camisa,
                camiseta,
                gorra,
                jersey,
                pantalon,
                sudadera
        ));
    }

    private Product createClothing(
            String name,
            String description,
            String price,
            String imageName,
            String category) {

        Product product = new Product(
                name,
                description,
                new BigDecimal(price),
                "/images/products/" + imageName,
                category
        );

        product.updateStock(ClothingSize.S, 5);
        product.updateStock(ClothingSize.M, 8);
        product.updateStock(ClothingSize.L, 4);
        product.updateStock(ClothingSize.XL, 0);

        return product;
    }
}