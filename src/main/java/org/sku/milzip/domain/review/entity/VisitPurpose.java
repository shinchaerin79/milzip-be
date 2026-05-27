package org.sku.milzip.domain.review.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VisitPurpose {
  DATE("데이트"),
  OUTING("외출"),
  OVERNIGHT_PASS("외박"),
  VACATION("휴가"),
  GATHERING("회식");

  private final String label;
}
