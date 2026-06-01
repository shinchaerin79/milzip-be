package org.sku.milzip.domain.benefit.repository;

import java.util.List;

import org.sku.milzip.domain.benefit.entity.Benefit;
import org.sku.milzip.domain.benefit.entity.BenefitType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BenefitRepository extends JpaRepository<Benefit, Long> {

  List<Benefit> findByBenefitTypeOrderByIdAsc(BenefitType benefitType);
}
