package org.sku.milzip.domain.review.repository;

import org.sku.milzip.domain.review.entity.Review;
import org.sku.milzip.domain.review.entity.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

  @Query(
      "SELECT r FROM Review r JOIN FETCH r.user WHERE r.store.id = :storeId AND r.status = :status")
  Page<Review> findByStoreIdAndStatus(
      @Param("storeId") Long storeId, @Param("status") ReviewStatus status, Pageable pageable);

  boolean existsByStoreIdAndUserId(Long storeId, Long userId);
}
