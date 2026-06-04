package org.sku.milzip.domain.benefit.repository;

import java.util.List;

import org.sku.milzip.domain.benefit.entity.Benefit;
import org.sku.milzip.domain.benefit.entity.BenefitType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BenefitRepository extends JpaRepository<Benefit, Long> {

  List<Benefit> findByBenefitTypeOrderByIdAsc(BenefitType benefitType);

  Page<Benefit> findByBenefitTypeOrderByIdAsc(BenefitType benefitType, Pageable pageable);

  Page<Benefit> findByBenefitTypeAndCategoryOrderByIdAsc(
      BenefitType benefitType, String category, Pageable pageable);
}
