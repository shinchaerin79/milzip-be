package org.sku.milzip.domain.benefit.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "자기계발 혜택 응답")
public class SelfDevelopmentBenefitResponse {

  @Schema(description = "혜택 ID")
  private Long id;

  @Schema(description = "프로그램명")
  private String title;

  @Schema(description = "설명")
  private String description;

  @Schema(description = "이미지 URL")
  private String imageUrl;

  @Schema(description = "카테고리", example = "학업")
  private String category;

  @Schema(description = "신청 URL")
  private String applyUrl;

  @Schema(description = "지원 유형", example = "무료")
  private String supportType;
}
