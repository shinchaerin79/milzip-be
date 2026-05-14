package org.sku.milzip.domain.auth.repository;

import java.util.Optional;

import org.sku.milzip.domain.auth.entity.EmailVerification;
import org.sku.milzip.domain.auth.entity.VerificationType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

  // 가장 최근에 발송된 코드 조회
  Optional<EmailVerification> findTopByEmailAndTypeOrderByIdDesc(
      String email, VerificationType type);

  void deleteByEmailAndType(String email, VerificationType type);
}
