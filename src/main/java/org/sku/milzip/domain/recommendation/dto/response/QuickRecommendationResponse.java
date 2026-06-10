package org.sku.milzip.domain.recommendation.dto.response;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

import org.sku.milzip.domain.store.entity.Store;
import org.sku.milzip.domain.store.entity.StoreCategory;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "빠른 추천 매장")
public class QuickRecommendationResponse {

  @Schema(description = "매장 ID", example = "1")
  private Long id;

  @Schema(description = "매장명", example = "아트밸리 자작나무")
  private String name;

  @Schema(description = "카테고리")
  private StoreCategory category;

  @Schema(description = "주소", example = "경기도 포천시 신북면 아트밸리로 42")
  private String address;

  @Schema(description = "위도", example = "37.9162")
  private Double latitude;

  @Schema(description = "경도", example = "127.1948")
  private Double longitude;

  @Schema(description = "전화번호", example = "031-534-9784")
  private String phone;

  @Schema(description = "군장병 혜택 매장 여부", example = "true")
  private boolean isMilitaryBenefit;

  @Schema(description = "혜택 인증 여부", example = "false")
  private boolean isBenefitVerified;

  @Schema(description = "최대 할인율 (%)", example = "10")
  private Integer maxDiscountRate;

  @Schema(description = "조회수", example = "142")
  private int viewCount;

  @Schema(description = "영업 시작 시간", example = "08:30")
  private LocalTime openTime;

  @Schema(description = "영업 종료 시간", example = "20:30")
  private LocalTime closeTime;

  @Schema(description = "현재 위치로부터의 거리 (km)", example = "1.2")
  private Double distanceKm;

  @Schema(description = "예상 이동시간 (분)", example = "18")
  private Integer travelTimeMinutes;

  @Schema(description = "이동 수단 (도보 / 차량)", example = "도보")
  private String travelMode;

  @Schema(description = "이미지 URL 목록")
  private List<String> imageUrls;

  private static List<String> extractImageUrls(Store store) {
    return store.getImages().stream()
        .sorted(
            java.util.Comparator.comparingInt(
                org.sku.milzip.domain.store.entity.StoreImage::getDisplayOrder))
        .map(org.sku.milzip.domain.store.entity.StoreImage::getImageUrl)
        .toList();
  }

  public static QuickRecommendationResponse from(
      Store store, double distanceKm, int travelTimeMinutes, String travelMode) {
    return new QuickRecommendationResponse(
        store.getId(),
        store.getName(),
        store.getCategory(),
        store.getAddress(),
        store.getLatitude(),
        store.getLongitude(),
        store.getPhone(),
        store.isMilitaryBenefit(),
        store.isBenefitVerified(),
        maxDiscount(store),
        store.getViewCount(),
        store.getOpenTime(),
        store.getCloseTime(),
        distanceKm,
        travelTimeMinutes,
        travelMode,
        extractImageUrls(store));
  }

  public static QuickRecommendationResponse from(Store store) {
    return new QuickRecommendationResponse(
        store.getId(),
        store.getName(),
        store.getCategory(),
        store.getAddress(),
        store.getLatitude(),
        store.getLongitude(),
        store.getPhone(),
        store.isMilitaryBenefit(),
        store.isBenefitVerified(),
        maxDiscount(store),
        store.getViewCount(),
        store.getOpenTime(),
        store.getCloseTime(),
        null,
        null,
        null,
        extractImageUrls(store));
  }

  private static Integer maxDiscount(Store store) {
    return store.getBenefits().stream()
        .map(b -> b.getDiscountRate())
        .filter(Objects::nonNull)
        .max(Integer::compareTo)
        .orElse(null);
  }
}
