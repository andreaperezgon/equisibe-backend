package com.equisibe.config;

import com.equisibe.model.ClothingSize;
import com.equisibe.model.Product;
import com.equisibe.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("demo")
public class DemoProductConfig {

    @Bean
    public CommandLineRunner loadDemoProducts(
            ProductRepository productRepository) {

        return args -> {
            if (productRepository.count() > 0) {
                return;
            }

            var shirt = clothing(
                    "Camisa Equisibé", "49.90",
                    "CAMISA.jpeg", "Camisas"
            );

            var tShirt = clothing(
                    "Camiseta Equisibé", "29.90",
                    "CAMISETA.jpeg", "Camisetas"
            );

            var sweater = clothing(
                    "Jersey Equisibé", "59.90",
                    "JERSEY.png", "Jerséis"
            );

            var trousers = clothing(
                    "Pantalón Equisibé", "69.90",
                    "PANTALON.jpeg", "Pantalones"
            );

            var sweatshirt = clothing(
                    "Sudadera Equisibé", "54.90",
                    "SUDADERA.jpeg", "Sudaderas"
            );

            var cap = new Product(
                    "Gorra Equisibé",
                    "Gorra Equisibé de talla única.",
                    new BigDecimal("24.90"),
                    "/images/products/GORRA.jpeg",
                    "Gorras"
            );

            cap.updateStock(ClothingSize.TU, 5);

            productRepository.saveAll(List.of(
                    shirt,
                    tShirt,
                    sweater,
                    trousers,
                    sweatshirt,
                    cap
            ));
        };
    }

    private Product clothing(
            String name,
            String price,
            String image,
            String category) {

        var product = new Product(
                name,
                name + ". Disponible en tallas S, M, L y XL.",
                new BigDecimal(price),
                "/images/products/" + image,
                category
        );

        product.updateStock(ClothingSize.S, 3);
        product.updateStock(ClothingSize.M, 0);
        product.updateStock(ClothingSize.L, 4);
        product.updateStock(ClothingSize.XL, 2);

        return product;
    }
}