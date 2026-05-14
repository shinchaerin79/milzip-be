package org.sku.milzip.domain.auth.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long memberId;

  @Column(nullable = false, unique = true)
  private String token;

  @Column(nullable = false)
  private LocalDateTime expiresAt;

  public static RefreshToken create(Long memberId, String token, long expirationMs) {
    RefreshToken rt = new RefreshToken();
    rt.memberId = memberId;
    rt.token = token;
    rt.expiresAt = LocalDateTime.now().plusSeconds(expirationMs / 1000);
    return rt;
  }

  public boolean isExpired() {
    return LocalDateTime.now().isAfter(expiresAt);
  }

  public void rotate(String newToken, long expirationMs) {
    this.token = newToken;
    this.expiresAt = LocalDateTime.now().plusSeconds(expirationMs / 1000);
  }
}
