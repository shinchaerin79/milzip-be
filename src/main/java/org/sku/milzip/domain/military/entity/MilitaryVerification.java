package org.sku.milzip.domain.military.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.sku.milzip.global.common.BaseTimeEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "military_verifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MilitaryVerification extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long userId;

  // 2차 요청에 필요한 파라미터 (사용 후 삭제)
  @Column(nullable = false)
  private String identity;

  @Column(nullable = false)
  private String phoneNo;

  @Column(nullable = false)
  private String addrSido;

  @Column(nullable = false)
  private String addrSigungu;

  // twoWayInfo
  private Integer jobIndex;
  private Integer threadIndex;
  private String jti;
  private Long twoWayTimestamp;

  @Column(nullable = false)
  private LocalDateTime expiresAt;

  public static MilitaryVerification create(
      Long userId, String identity, String phoneNo, String addrSido, String addrSigungu) {
    MilitaryVerification mv = new MilitaryVerification();
    mv.userId = userId;
    mv.identity = identity;
    mv.phoneNo = phoneNo;
    mv.addrSido = addrSido;
    mv.addrSigungu = addrSigungu;
    mv.expiresAt = LocalDateTime.now().plusMinutes(10);
    return mv;
  }

  public void updateTwoWayInfo(int jobIndex, int threadIndex, String jti, long twoWayTimestamp) {
    this.jobIndex = jobIndex;
    this.threadIndex = threadIndex;
    this.jti = jti;
    this.twoWayTimestamp = twoWayTimestamp;
  }

  public boolean isExpired() {
    return LocalDateTime.now().isAfter(expiresAt);
  }
}
