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

  private static final java.util.List<String> CAFE_KEYWORDS =
      java.util.List.of("카페", "커피", "coffee", "cafe", "다방", "베이커리", "bakery", "빵");

  public static StoreCategory resolve(StoreCategory original, String storeName) {
    if (original != FOOD || storeName == null) return original;
    String lower = storeName.toLowerCase();
    for (String keyword : CAFE_KEYWORDS) {
      if (lower.contains(keyword)) return CAFE;
    }
    return original;
  }

  /** category 필터링 시 DB에서 조회해야 할 실제 카테고리 목록을 반환합니다. */
  public static java.util.List<StoreCategory> dbCategoriesFor(StoreCategory requested) {
    if (requested == CAFE) return java.util.List.of(CAFE, FOOD);
    return java.util.List.of(requested);
  }
}
