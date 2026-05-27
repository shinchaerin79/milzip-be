package org.sku.milzip.domain.review.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VisitType {
  WALK_IN("예약 없이 이용"),
  RESERVED("예약 후 이용"),
  TAKEOUT_DELIVERY("포장 및 배달 이용");

  private final String label;
}
