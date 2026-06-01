package org.sku.milzip.domain.store.dto.response;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

import org.sku.milzip.domain.store.entity.Store;
import org.sku.milzip.domain.store.entity.StoreCategory;
import org.sku.milzip.domain.store.entity.StoreImage;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "매장 목록 항목")
public class StoreListItemResponse {

  @Schema(description = "매장 ID", example = "1")
  private Long id;

  @Schema(description = "매장명", example = "아트밸리 자작나무")
  private String name;

  @Schema(description = "카테고리 (FOOD / CAFE / LEISURE / ACCOMMODATION / ETC)")
  private StoreCategory category;

  @Schema(description = "주소", example = "경기도 포천시 신북면 아트밸리로 42")
  private String address;

  @Schema(description = "위도", example = "37.89257701967812")
  private Double latitude;

  @Schema(description = "경도", example = "127.19789570920469")
  private Double longitude;

  @Schema(description = "전화번호", example = "031-534-9784")
  private String phone;

  @Schema(description = "군장병 혜택 매장 여부", example = "true")
  private boolean isMilitaryBenefit;

  @Schema(description = "혜택 인증 여부", example = "false")
  private boolean isBenefitVerified;

  @Schema(description = "조회수", example = "142")
  private int viewCount;

  @Schema(description = "영업 시작 시간", example = "08:30")
  private LocalTime openTime;

  @Schema(description = "영업 종료 시간", example = "20:30")
  private LocalTime closeTime;

  @Schema(description = "최대 할인율 (%)", example = "10")
  private Integer maxDiscountRate;

  @Schema(description = "현재 위치로부터의 거리 (km). sortBy=distance 요청 시에만 포함", example = "3.2")
  private Double distanceKm;

  @Schema(description = "이미지 URL 목록")
  private List<String> imageUrls;

  private static List<String> extractImageUrls(Store store) {
    return store.getImages().stream()
        .sorted(Comparator.comparingInt(StoreImage::getDisplayOrder))
        .map(StoreImage::getImageUrl)
        .toList();
  }

  public static StoreListItemResponse from(Store store) {
    Integer maxDiscount =
        store.getBenefits().stream()
            .map(StoreBenefitResponse::from)
            .map(StoreBenefitResponse::getDiscountRate)
            .filter(r -> r != null)
            .max(Integer::compareTo)
            .orElse(null);

    return new StoreListItemResponse(
        store.getId(),
        store.getName(),
        store.getCategory(),
        store.getAddress(),
        store.getLatitude(),
        store.getLongitude(),
        store.getPhone(),
        store.isMilitaryBenefit(),
        store.isBenefitVerified(),
        store.getViewCount(),
        store.getOpenTime(),
        store.getCloseTime(),
        maxDiscount,
        null,
        extractImageUrls(store));
  }

  public static StoreListItemResponse from(Store store, double distanceKm) {
    StoreListItemResponse base = from(store);
    return new StoreListItemResponse(
        base.id,
        base.name,
        base.category,
        base.address,
        base.latitude,
        base.longitude,
        base.phone,
        base.isMilitaryBenefit,
        base.isBenefitVerified,
        base.viewCount,
        base.openTime,
        base.closeTime,
        base.maxDiscountRate,
        distanceKm,
        base.imageUrls);
  }
}
