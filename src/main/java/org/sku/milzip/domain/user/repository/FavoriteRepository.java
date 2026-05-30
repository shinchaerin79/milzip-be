package org.sku.milzip.domain.user.repository;

import java.util.List;
import java.util.Optional;

import org.sku.milzip.domain.user.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

  @Query(
      "SELECT f FROM Favorite f JOIN FETCH f.store s LEFT JOIN FETCH s.benefits WHERE f.user.id = :userId")
  List<Favorite> findByUserIdWithStore(@Param("userId") Long userId);

  Optional<Favorite> findByUserIdAndStoreId(Long userId, Long storeId);

  boolean existsByUserIdAndStoreId(Long userId, Long storeId);
}
