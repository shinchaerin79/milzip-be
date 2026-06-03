package org.sku.milzip.domain.benefit.dto.response;

import java.time.LocalDate;

import org.sku.milzip.domain.benefit.entity.Benefit;
import org.sku.milzip.domain.benefit.entity.BenefitType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "혜택 응답")
public class BenefitResponse {

  @Schema(description = "혜택 ID")
  private Long id;

  @Schema(description = "혜택 유형")
  private BenefitType benefitType;

  @Schema(description = "혜택 유형 한글명")
  private String benefitTypeDescription;

  @Schema(description = "제목")
  private String title;

  @Schema(description = "설명")
  private String description;

  @Schema(description = "이미지 URL")
  private String imageUrl;

  // 영화 혜택
  @Schema(description = "영화관 체인")
  private String cinemaChain;

  // 놀이공원 혜택
  @Schema(description = "지역")
  private String region;

  @Schema(description = "위치")
  private String location;

  @Schema(description = "혜택 시작일")
  private LocalDate validFrom;

  @Schema(description = "혜택 종료일")
  private LocalDate validUntil;

  @Schema(description = "원래 가격 (원)")
  private Integer originalPrice;

  @Schema(description = "할인 가격 (원)")
  private Integer discountedPrice;

  @Schema(description = "할인 설명")
  private String discountDescription;

  @Schema(description = "증명 방법")
  private String verificationMethod;

  // 자기계발 혜택
  @Schema(description = "카테고리")
  private String category;

  @Schema(description = "신청 URL")
  private String applyUrl;

  @Schema(description = "지원 유형")
  private String supportType;

  @Schema(description = "담당 기관")
  private String superviseInst;

  public static BenefitResponse from(Benefit benefit) {
    return BenefitResponse.builder()
        .id(benefit.getId())
        .benefitType(benefit.getBenefitType())
        .benefitTypeDescription(benefit.getBenefitType().getDescription())
        .title(benefit.getTitle())
        .description(benefit.getDescription())
        .imageUrl(benefit.getImageUrl())
        .cinemaChain(benefit.getCinemaChain())
        .region(benefit.getRegion())
        .location(benefit.getLocation())
        .validFrom(benefit.getValidFrom())
        .validUntil(benefit.getValidUntil())
        .originalPrice(benefit.getOriginalPrice())
        .discountedPrice(benefit.getDiscountedPrice())
        .discountDescription(benefit.getDiscountDescription())
        .verificationMethod(benefit.getVerificationMethod())
        .category(benefit.getCategory())
        .applyUrl(benefit.getApplyUrl())
        .supportType(benefit.getSupportType())
        .superviseInst(benefit.getSuperviseInst())
        .build();
  }
}
