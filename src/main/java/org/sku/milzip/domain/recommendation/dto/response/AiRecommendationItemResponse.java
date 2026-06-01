package org.sku.milzip.domain.recommendation.dto.response;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.sku.milzip.domain.store.entity.Store;
import org.sku.milzip.domain.store.entity.StoreCategory;
import org.sku.milzip.domain.store.entity.StoreImage;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "AI 맞춤 추천 매장")
public class AiRecommendationItemResponse {

  @Schema(description = "매장 ID", example = "1")
  private Long id;

  @Schema(description = "매장명", example = "맛있는 삼겹살집")
  private String name;

  @Schema(description = "카테고리")
  private StoreCategory category;

  @Schema(description = "주소", example = "서울특별시 강남구 역삼동 123-45")
  private String address;

  @Schema(description = "전화번호", example = "02-1234-5678")
  private String phone;

  @Schema(description = "위도", example = "37.89257701967812")
  private Double latitude;

  @Schema(description = "경도", example = "127.19789570920469")
  private Double longitude;

  @Schema(description = "최대 할인율 (%)", example = "20")
  private Integer maxDiscountRate;

  @Schema(description = "영업 시작 시간", example = "11:00")
  private LocalTime openTime;

  @Schema(description = "영업 종료 시간", example = "22:00")
  private LocalTime closeTime;

  @Schema(description = "현재 위치로부터의 거리 (km)", example = "1.3")
  private Double distanceKm;

  @Schema(description = "예상 이동시간 (분)", example = "20")
  private Integer travelTimeMinutes;

  @Schema(description = "이동 수단 (도보 / 차량)", example = "도보")
  private String travelMode;

  @Schema(description = "AI 추천 이유", example = "친구와 함께 가기 좋은 분위기의 삼겹살 맛집으로, 군장병 20% 할인 혜택이 있습니다.")
  private String reason;

  @Schema(description = "이미지 URL 목록")
  private List<String> imageUrls;

  public static AiRecommendationItemResponse of(
      Store store, String reason, Double distanceKm, Integer travelTimeMinutes, String travelMode) {
    return AiRecommendationItemResponse.builder()
        .id(store.getId())
        .name(store.getName())
        .category(store.getCategory())
        .address(store.getAddress())
        .phone(store.getPhone())
        .latitude(store.getLatitude())
        .longitude(store.getLongitude())
        .maxDiscountRate(maxDiscount(store))
        .openTime(store.getOpenTime())
        .closeTime(store.getCloseTime())
        .distanceKm(distanceKm)
        .travelTimeMinutes(travelTimeMinutes)
        .travelMode(travelMode)
        .reason(reason)
        .imageUrls(
            store.getImages().stream()
                .sorted(Comparator.comparingInt(StoreImage::getDisplayOrder))
                .map(StoreImage::getImageUrl)
                .toList())
        .build();
  }

  private static Integer maxDiscount(Store store) {
    return store.getBenefits().stream()
        .map(b -> b.getDiscountRate())
        .filter(Objects::nonNull)
        .max(Integer::compareTo)
        .orElse(null);
  }
}
