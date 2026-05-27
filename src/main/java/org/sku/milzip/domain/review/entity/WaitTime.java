package org.sku.milzip.domain.review.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WaitTime {
  IMMEDIATE("바로 입장"),
  WITHIN_10_MIN("10분 이내"),
  WITHIN_30_MIN("30분 이내"),
  WITHIN_1_HOUR("1시간 이내"),
  OVER_1_HOUR("1시간 이상");

  private final String label;
}
