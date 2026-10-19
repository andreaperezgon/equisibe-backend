package com.equisibe.service;

import com.equisibe.dto.ProductResponse;
import com.equisibe.model.Favorite;
import com.equisibe.model.Product;
import com.equisibe.model.User;
import com.equisibe.repository.FavoriteRepository;
import com.equisibe.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;
    private final EntityManager entityManager;

    public FavoriteService(
            FavoriteRepository favoriteRepository,
            ProductRepository productRepository,
            EntityManager entityManager) {
        this.favoriteRepository = favoriteRepository;
        this.productRepository = productRepository;
        this.entityManager = entityManager;
    }

    public List<ProductResponse> getFavorites(String email) {
        User user = getActiveUser(email, false);

        return favoriteRepository
                .findByUser_IdAndProduct_ActiveTrueOrderByIdDesc(user.getId())
                .stream()
                .map(favorite -> toResponse(favorite.getProduct()))
                .toList();
    }

    @Transactional
    public void addFavorite(String email, Long productId) {
        validateProductId(productId);
        User user = getActiveUser(email, true);

        Product product = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "La prenda no está disponible."
                ));

        if (!favoriteRepository.existsByUser_IdAndProduct_Id(
                user.getId(), productId)) {
            favoriteRepository.saveAndFlush(new Favorite(user, product));
        }
    }

    @Transactional
    public void removeFavorite(String email, Long productId) {
        validateProductId(productId);
        User user = getActiveUser(email, true);

        favoriteRepository.deleteByUser_IdAndProduct_Id(
                user.getId(), productId
        );
    }

    private User getActiveUser(String email, boolean forUpdate) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Debes iniciar sesión."
            );
        }

        var query = entityManager.createQuery(
                "select u from User u where u.email = :email "
                        + "and u.enabled = true",
                User.class
        ).setParameter("email", email);

        if (forUpdate) {
            query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        }

        return query.getResultList().stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "La sesión no es válida."
                ));
    }

    private void validateProductId(Long productId) {
        if (productId == null || productId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador de la prenda no es válido."
            );
        }
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getEffectivePrice(),
                product.getImageUrl(),
                product.getCategory(),
                product.getStockBySize(),
                product.getEffectivePrice().compareTo(product.getPrice()) < 0
                        ? product.getPrice() : null
        );
    }
}
