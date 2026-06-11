package org.sku.milzip.domain.store.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StoreCategory {
  // DB 저장 값
  FOOD("음식"),
  CAFE("카페"),
  LEISURE("여가"),
  ACCOMMODATION("숙박"),
  ETC("기타"),
  // 프론트 필터/표시 전용 (DB에 저장되지 않음)
  PC_CAFE("PC방"),
  SERVICE("서비스");

  private final String label;

  private static final java.util.List<String> PC_KEYWORDS =
      java.util.List.of("pc방", "pc 방", "피씨방", "피씨 방", "pc");

  /** DB 저장 카테고리를 프론트 표시 카테고리로 변환 */
  public static StoreCategory resolve(StoreCategory original, String storeName) {
    return switch (original) {
      case CAFE -> FOOD;
      case LEISURE, ETC -> {
        if (storeName == null) yield SERVICE;
        String lower = storeName.toLowerCase();
        yield PC_KEYWORDS.stream().anyMatch(lower::contains) ? PC_CAFE : SERVICE;
      }
      default -> original;
    };
  }

  /** category 필터링 시 DB에서 조회해야 할 실제 카테고리 목록을 반환 */
  public static java.util.List<StoreCategory> dbCategoriesFor(StoreCategory requested) {
    return switch (requested) {
      case FOOD -> java.util.List.of(FOOD, CAFE);
      case PC_CAFE, SERVICE -> java.util.List.of(LEISURE, ETC);
      default -> java.util.List.of(requested);
    };
  }
}
