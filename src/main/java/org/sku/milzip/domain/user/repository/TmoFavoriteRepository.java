package org.sku.milzip.domain.user.repository;

import java.util.List;
import java.util.Optional;

import org.sku.milzip.domain.user.entity.TmoFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TmoFavoriteRepository extends JpaRepository<TmoFavorite, Long> {

  @Query("SELECT tf FROM TmoFavorite tf JOIN FETCH tf.tmo WHERE tf.user.id = :userId")
  List<TmoFavorite> findByUserIdWithTmo(@Param("userId") Long userId);

  Optional<TmoFavorite> findByUserIdAndTmoId(Long userId, Long tmoId);

  boolean existsByUserIdAndTmoId(Long userId, Long tmoId);
}
