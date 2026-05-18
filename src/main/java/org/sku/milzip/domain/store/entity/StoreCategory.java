package org.sku.milzip.domain.store.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StoreCategory {
  FOOD("음식"),
  CAFE("카페"),
  LEISURE("여가"),
  ACCOMMODATION("숙박"),
  ETC("기타");

  private final String label;
}
