package com.equisibe.config;

import com.equisibe.model.ClothingSize;
import com.equisibe.model.Order;
import com.equisibe.model.OrderItem;
import com.equisibe.model.OrderShippingAddress;
import com.equisibe.model.Product;
import com.equisibe.model.ReturnItem;
import com.equisibe.model.ReturnRequest;
import com.equisibe.model.ReturnType;
import com.equisibe.model.Role;
import com.equisibe.model.User;
import com.equisibe.repository.OrderRepository;
import com.equisibe.repository.ProductRepository;
import com.equisibe.repository.ReturnRequestRepository;
import com.equisibe.repository.UserRepository;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/** Datos ficticios: esta configuración solo existe en el classpath de pruebas. */
@Configuration
@Profile("e2e")
public class E2eFixturesConfig {
    @Bean
    CommandLineRunner e2eFixtures(DataSource dataSource, Environment environment,
            UserRepository users, ProductRepository products, OrderRepository orders,
            ReturnRequestRepository returns, PasswordEncoder encoder) {
        return args -> {
            if (!environment.matchesProfiles("test")) {
                throw new IllegalStateException("E2E requiere también el perfil test.");
            }
            try (var connection = dataSource.getConnection()) {
                if (!connection.getMetaData().getURL().startsWith("jdbc:h2:mem:equisibe_e2e")) {
                    throw new IllegalStateException("E2E solo puede usar su base de datos temporal.");
                }
            }
            String password = encoder.encode("E2eOnly!2026");
            var admin = new User("Administración E2E", "e2e-admin@example.test", password);
            ReflectionTestUtils.setField(admin, "role", Role.ADMIN);
            users.saveAndFlush(admin);
            var customer = users.saveAndFlush(new User("Cliente con pedido E2E",
                    "e2e-customer@example.test", password));

            var shirt = new Product("Camisa E2E", "Prenda ficticia para las pruebas.",
                    new BigDecimal("40.00"), "/images/products/CAMISA.jpeg", "Camisas");
            shirt.updateStock(ClothingSize.S, 5);
            shirt.updateStock(ClothingSize.M, 4);
            products.saveAndFlush(shirt);
            var cap = new Product("Gorra E2E", "Gorra ficticia para las pruebas.",
                    new BigDecimal("24.90"), "/images/products/GORRA.jpeg", "Gorras");
            cap.updateStock(ClothingSize.TU, 5);
            products.saveAndFlush(cap);

            var address = new OrderShippingAddress("Cliente E2E", "600000000", "Calle de prueba 1",
                    null, "Oviedo", "Asturias", "33001", "ES");
            var order = new Order(customer, address,
                    List.of(new OrderItem(shirt.getId(), shirt.getName(), ClothingSize.S, 1,
                            new BigDecimal("40.00"))), new BigDecimal("3.95"));
            order.markPaid();
            orders.saveAndFlush(order);
            returns.saveAndFlush(new ReturnRequest(UUID.randomUUID(), order, ReturnType.WITHDRAWAL,
                    "Solicitud ficticia E2E", List.of(new ReturnItem(0, 1))));
        };
    }

    @Bean
    FilterRegistrationBean<Filter> e2eMarker() {
        Filter marker = (request, response, chain) -> {
            ((HttpServletResponse) response).setHeader("X-Equisibe-E2E", "isolated-memory");
            chain.doFilter(request, response);
        };
        var registration = new FilterRegistrationBean<>(marker);
        registration.setOrder(Integer.MIN_VALUE);
        return registration;
    }
}
