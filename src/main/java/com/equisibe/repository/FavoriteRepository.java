package com.equisibe.repository;

import com.equisibe.model.Favorite;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    @EntityGraph(attributePaths = "product")
    List<Favorite> findByUser_IdAndProduct_ActiveTrueOrderByIdDesc(
            Long userId
    );

    boolean existsByUser_IdAndProduct_Id(Long userId, Long productId);

    long deleteByUser_IdAndProduct_Id(Long userId, Long productId);
}
