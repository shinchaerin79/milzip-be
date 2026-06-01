package org.sku.milzip.domain.benefit.dto.request;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "자기계발 혜택 등록/수정 요청")
public class SelfDevelopmentBenefitRequest {

  @NotBlank
  @Schema(description = "프로그램명", example = "군 e-러닝 (나라사랑포털)")
  private String title;

  @Schema(description = "설명", example = "어학·자격증·취업·IT 등 1만여 강좌 무료")
  private String description;

  @Schema(description = "이미지 URL")
  private String imageUrl;

  @Schema(
      description = "카테고리",
      example = "학업",
      allowableValues = {"학업", "자격증", "취업", "지원금", "학원"})
  private String category;

  @Schema(description = "신청 URL")
  private String applyUrl;

  @Schema(
      description = "지원 유형",
      example = "무료",
      allowableValues = {"무료", "할인", "지원금"})
  private String supportType;
}
