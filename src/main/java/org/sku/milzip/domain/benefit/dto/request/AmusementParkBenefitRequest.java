package org.sku.milzip.domain.benefit.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "놀이공원 혜택 등록/수정 요청")
public class AmusementParkBenefitRequest {

  @NotBlank
  @Schema(description = "시설명", example = "에버랜드")
  private String title;

  @Schema(description = "설명", example = "휴가 군인·공익 본인 한정 1회 무료 자유이용권")
  private String description;

  @Schema(description = "이미지 URL")
  private String imageUrl;

  @Schema(description = "지역", example = "수도권")
  private String region;

  @Schema(description = "위치", example = "경기 용인")
  private String location;

  @Schema(description = "혜택 시작일")
  private LocalDate validFrom;

  @Schema(description = "혜택 종료일", example = "2026-09-30")
  private LocalDate validUntil;

  @Schema(description = "원래 가격 (원)", example = "62000")
  private Integer originalPrice;

  @Schema(description = "할인 가격 (원, 0이면 무료)", example = "0")
  private Integer discountedPrice;

  @Schema(description = "할인 설명", example = "무료")
  private String discountDescription;

  @Schema(description = "증명 방법", example = "휴가증, 병력증명서")
  private String verificationMethod;
}
