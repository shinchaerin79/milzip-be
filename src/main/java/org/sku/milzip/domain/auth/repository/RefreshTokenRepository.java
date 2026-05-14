package org.sku.milzip.domain.auth.repository;

import java.util.Optional;

import org.sku.milzip.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByToken(String token);

  Optional<RefreshToken> findByMemberId(Long memberId);

  void deleteByMemberId(Long memberId);
}
