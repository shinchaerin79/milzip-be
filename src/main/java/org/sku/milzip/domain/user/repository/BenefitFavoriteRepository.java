package org.sku.milzip.domain.user.repository;

import java.util.List;
import java.util.Optional;

import org.sku.milzip.domain.user.entity.BenefitFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BenefitFavoriteRepository extends JpaRepository<BenefitFavorite, Long> {

  @Query("SELECT bf FROM BenefitFavorite bf JOIN FETCH bf.benefit WHERE bf.user.id = :userId")
  List<BenefitFavorite> findByUserIdWithBenefit(@Param("userId") Long userId);

  Optional<BenefitFavorite> findByUserIdAndBenefitId(Long userId, Long benefitId);

  boolean existsByUserIdAndBenefitId(Long userId, Long benefitId);
}
