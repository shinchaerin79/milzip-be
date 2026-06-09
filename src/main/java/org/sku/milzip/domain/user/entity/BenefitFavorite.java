package org.sku.milzip.domain.user.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.sku.milzip.domain.benefit.entity.Benefit;
import org.sku.milzip.global.common.BaseTimeEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "benefit_favorites",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "benefit_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BenefitFavorite extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "benefit_id", nullable = false)
  private Benefit benefit;

  public static BenefitFavorite create(User user, Benefit benefit) {
    BenefitFavorite bf = new BenefitFavorite();
    bf.user = user;
    bf.benefit = benefit;
    return bf;
  }
}
