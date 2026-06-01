package org.sku.milzip.domain.store.dto.response;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.sku.milzip.domain.store.entity.Store;
import org.sku.milzip.domain.store.entity.StoreCategory;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "매장 상세 정보")
public class StoreDetailResponse {

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

  @Schema(description = "매장 종료일 (임시 휴업 등)", example = "2025-12-31T00:00:00")
  private LocalDateTime closeDate;

  @Schema(description = "이미지 URL 목록")
  private List<String> imageUrls;

  @Schema(description = "군장병 혜택 목록")
  private List<StoreBenefitResponse> benefits;

  public static StoreDetailResponse from(Store store) {
    return new StoreDetailResponse(
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
        store.getCloseDate(),
        store.getImages().stream()
            .sorted(
                java.util.Comparator.comparingInt(
                    org.sku.milzip.domain.store.entity.StoreImage::getDisplayOrder))
            .map(org.sku.milzip.domain.store.entity.StoreImage::getImageUrl)
            .toList(),
        store.getBenefits().stream().map(StoreBenefitResponse::from).toList());
  }
}
