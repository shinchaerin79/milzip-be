package org.sku.milzip.domain.military.repository;

import java.util.Optional;

import org.sku.milzip.domain.military.entity.MilitaryVerification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MilitaryVerificationRepository extends JpaRepository<MilitaryVerification, Long> {

  Optional<MilitaryVerification> findByUserId(Long userId);

  void deleteByUserId(Long userId);
}
