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
  // 지도 페이지 필터 전용 (DB에 저장되지 않음)
  PC_CAFE("PC방"),
  SERVICE("서비스");

  private final String label;

  /** 기존 카페 키워드 매핑 (FOOD → CAFE 변환용) */
  private static final java.util.List<String> CAFE_KEYWORDS =
      java.util.List.of("카페", "커피", "coffee", "cafe", "다방", "베이커리", "bakery", "빵");

  /** PC방 판별 키워드 */
  private static final java.util.List<String> PC_KEYWORDS =
      java.util.List.of("pc방", "pc 방", "피씨방", "피씨 방", "pc");

  /** DB 저장 카테고리 → 응답 표시 카테고리 변환 (기존 동작 유지) */
  public static StoreCategory resolve(StoreCategory original, String storeName) {
    if (original != FOOD || storeName == null) return original;
    String lower = storeName.toLowerCase();
    for (String keyword : CAFE_KEYWORDS) {
      if (lower.contains(keyword)) return CAFE;
    }
    return original;
  }

  /** category 필터링 시 DB에서 조회해야 할 실제 카테고리 목록을 반환 */
  public static java.util.List<StoreCategory> dbCategoriesFor(StoreCategory requested) {
    return switch (requested) {
      case FOOD -> java.util.List.of(FOOD, CAFE);
      case CAFE -> java.util.List.of(CAFE, FOOD);
      case PC_CAFE, SERVICE -> java.util.List.of(LEISURE, ETC);
      default -> java.util.List.of(requested);
    };
  }

  /** 지도 페이지 필터 매칭 여부 판단 (기존 CAFE/LEISURE/ETC 등은 resolve 동작 유지) */
  public static boolean matchesFilter(
      StoreCategory dbCategory, String storeName, StoreCategory filter) {
    return switch (filter) {
      case FOOD -> dbCategory == FOOD || dbCategory == CAFE;
      case CAFE -> resolve(dbCategory, storeName) == CAFE;
      case PC_CAFE -> (dbCategory == LEISURE || dbCategory == ETC) && isPcStore(storeName);
      case SERVICE -> (dbCategory == LEISURE || dbCategory == ETC) && !isPcStore(storeName);
      default -> dbCategory == filter;
    };
  }

  private static boolean isPcStore(String name) {
    if (name == null) return false;
    String lower = name.toLowerCase();
    return PC_KEYWORDS.stream().anyMatch(lower::contains);
  }
}
