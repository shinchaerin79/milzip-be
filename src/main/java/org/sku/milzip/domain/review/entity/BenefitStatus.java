package org.sku.milzip.domain.review.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BenefitStatus {
  RECEIVED("혜택 받음"),
  NOT_RECEIVED("혜택 받지 못함"),
  PARTIAL("일부 받음");

  private final String label;
}
