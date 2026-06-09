package org.sku.milzip.domain.user.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.sku.milzip.domain.benefit.entity.BenefitType;
import org.sku.milzip.domain.user.entity.BenefitFavorite;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "혜택 즐겨찾기 응답")
public class BenefitFavoriteResponse {

  @Schema(description = "즐겨찾기 ID")
  private Long id;

  @Schema(description = "혜택 ID")
  private Long benefitId;

  @Schema(description = "혜택 유형")
  private BenefitType benefitType;

  @Schema(description = "혜택 유형 한글명")
  private String benefitTypeDescription;

  @Schema(description = "제목")
  private String title;

  @Schema(description = "이미지 URL")
  private String imageUrl;

  @Schema(description = "지역 (놀이공원)")
  private String region;

  @Schema(description = "할인 가격 (원) (놀이공원)")
  private Integer discountedPrice;

  @Schema(description = "할인 설명 (놀이공원)")
  private String discountDescription;

  @Schema(description = "혜택 종료일 (놀이공원)")
  private LocalDate validUntil;

  @Schema(description = "영화관 체인 (영화)")
  private String cinemaChain;

  @Schema(description = "신청 URL (자기계발)")
  private String applyUrl;

  @Schema(description = "즐겨찾기 추가 일시")
  private LocalDateTime createdAt;

  public static BenefitFavoriteResponse from(BenefitFavorite bf) {
    return BenefitFavoriteResponse.builder()
        .id(bf.getId())
        .benefitId(bf.getBenefit().getId())
        .benefitType(bf.getBenefit().getBenefitType())
        .benefitTypeDescription(bf.getBenefit().getBenefitType().getDescription())
        .title(bf.getBenefit().getTitle())
        .imageUrl(bf.getBenefit().getImageUrl())
        .region(bf.getBenefit().getRegion())
        .discountedPrice(bf.getBenefit().getDiscountedPrice())
        .discountDescription(bf.getBenefit().getDiscountDescription())
        .validUntil(bf.getBenefit().getValidUntil())
        .cinemaChain(bf.getBenefit().getCinemaChain())
        .applyUrl(bf.getBenefit().getApplyUrl())
        .createdAt(bf.getCreatedAt())
        .build();
  }
}
