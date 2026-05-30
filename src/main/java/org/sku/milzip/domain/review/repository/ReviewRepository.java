package org.sku.milzip.domain.review.repository;

import java.util.Optional;

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

  boolean existsByReceiptIdentifier(String receiptIdentifier);

  @Query(
      "SELECT r FROM Review r JOIN FETCH r.store JOIN FETCH r.user WHERE r.store.id = :storeId AND r.id = :reviewId")
  Optional<Review> findByStoreIdAndId(
      @Param("storeId") Long storeId, @Param("reviewId") Long reviewId);

  @Query(
      "SELECT r FROM Review r JOIN FETCH r.store WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
  Page<Review> findByUserId(@Param("userId") Long userId, Pageable pageable);
}
