package com.equisibe.controller;

import com.equisibe.dto.AdminProductRequest;
import com.equisibe.dto.AdminProductResponse;
import com.equisibe.service.AdminProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final AdminProductService productService;

    public AdminProductController(AdminProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<AdminProductResponse> getProducts(Principal principal) {
        return productService.getProducts(requireEmail(principal));
    }

    @GetMapping("/{productId}")
    public AdminProductResponse getProduct(
            @PathVariable("productId") Long productId,
            Principal principal) {
        return productService.getProduct(requireEmail(principal), productId);
    }

    @PostMapping
    public ResponseEntity<AdminProductResponse> createProduct(
            @Valid @RequestBody AdminProductRequest request,
            Principal principal) {
        var product = productService.createProduct(
                requireEmail(principal), request
        );

        return ResponseEntity
                .created(URI.create("/api/admin/products/" + product.id()))
                .body(product);
    }

    @PutMapping("/{productId}")
    public AdminProductResponse updateProduct(
            @PathVariable("productId") Long productId,
            @Valid @RequestBody AdminProductRequest request,
            Principal principal) {
        return productService.updateProduct(
                requireEmail(principal), productId, request
        );
    }

    @PutMapping("/{productId}/active")
    public AdminProductResponse setActive(
            @PathVariable("productId") Long productId,
            @Valid @RequestBody ActivationRequest request,
            Principal principal) {
        return productService.setActive(
                requireEmail(principal), productId, request.active()
        );
    }


    @PutMapping("/{productId}/stock")
    public AdminProductResponse updateStock(
            @PathVariable("productId") Long productId,
            @Valid @RequestBody com.equisibe.dto.AdminStockRequest request,
            Principal principal) {
        return productService.updateStock(
                requireEmail(principal), productId, request
        );
    }


    @PutMapping("/{productId}/sale")
    public AdminProductResponse updateSale(
            @PathVariable("productId") Long productId,
            @Valid @RequestBody com.equisibe.dto.AdminSaleRequest request,
            Principal principal) {
        return productService.updateSale(
                requireEmail(principal), productId, request
        );
    }

    private String requireEmail(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Debes iniciar sesión."
            );
        }

        return principal.getName();
    }

    public record ActivationRequest(
            @NotNull(message = "Indica si el producto debe estar activo.")
            Boolean active
    ) {
    }
}
