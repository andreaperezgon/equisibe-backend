package com.equisibe.service;

import com.equisibe.dto.AdminProductRequest;
import com.equisibe.dto.AdminProductResponse;
import com.equisibe.model.Product;
import com.equisibe.model.Role;
import com.equisibe.repository.ProductRepository;
import com.equisibe.repository.UserRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class AdminProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public AdminProductService(
            ProductRepository productRepository,
            UserRepository userRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    public List<AdminProductResponse> getProducts(String email) {
        requireAdmin(email);

        return productRepository
                .findAll(Sort.by(Sort.Direction.DESC, "id"))
                .stream()
                .map(AdminProductResponse::from)
                .toList();
    }

    public AdminProductResponse getProduct(String email, Long productId) {
        requireAdmin(email);
        return AdminProductResponse.from(findProduct(productId));
    }

    @Transactional
    public AdminProductResponse createProduct(
            String email,
            AdminProductRequest request) {
        requireAdmin(email);

        Product product = new Product(
                request.name().trim(),
                request.description().trim(),
                request.price().setScale(2),
                request.imageUrl().trim(),
                request.category().trim()
        );

        return AdminProductResponse.from(
                productRepository.saveAndFlush(product)
        );
    }

    @Transactional
    public AdminProductResponse updateProduct(
            String email,
            Long productId,
            AdminProductRequest request) {
        requireAdmin(email);
        Product product = findProduct(productId);

        product.updateDetails(
                request.name(),
                request.description(),
                request.price().setScale(2),
                request.imageUrl(),
                request.category()
        );

        return AdminProductResponse.from(
                productRepository.saveAndFlush(product)
        );
    }

    @Transactional
    public AdminProductResponse setActive(
            String email,
            Long productId,
            boolean active) {
        requireAdmin(email);
        Product product = findProduct(productId);
        product.setActive(active);

        return AdminProductResponse.from(
                productRepository.saveAndFlush(product)
        );
    }


    @Transactional
    public AdminProductResponse updateStock(
            String email,
            Long productId,
            com.equisibe.dto.AdminStockRequest request) {
        requireAdmin(email);
        Product product = findProduct(productId);
        var currentSizes = product.getStockBySize().keySet();
        var requestedSizes = request.stockBySize().keySet();

        if (!currentSizes.equals(requestedSizes)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debes enviar todas las tallas actuales del producto."
            );
        }

        request.stockBySize().forEach(product::updateStock);
        return AdminProductResponse.from(
                productRepository.saveAndFlush(product)
        );
    }


    @Transactional
    public AdminProductResponse updateSale(
            String email,
            Long productId,
            com.equisibe.dto.AdminSaleRequest request) {
        requireAdmin(email);
        Product product = findProduct(productId);
        try {
            product.updateSalePrice(request.salePrice());
        } catch (IllegalArgumentException failure) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El precio rebajado debe ser menor que el habitual."
            );
        }
        return AdminProductResponse.from(
                productRepository.saveAndFlush(product)
        );
    }

    private Product findProduct(Long productId) {
        if (productId == null || productId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador del producto no es válido."
            );
        }

        return productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El producto no existe."
                ));
    }

    private void requireAdmin(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Debes iniciar sesión."
            );
        }

        userRepository.findByEmail(email)
                .filter(user -> user.isEnabled()
                        && user.getRole() == Role.ADMIN)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Necesitas permisos de administrador."
                ));
    }
}
