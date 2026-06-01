package org.sku.milzip.domain.benefit.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BenefitType {
  MOVIE_BENEFIT("영화관 할인 혜택"),
  AMUSEMENT_PARK("놀이공원 혜택"),
  SELF_DEVELOPMENT("자기계발 혜택");

  private final String description;
}
