package org.sku.milzip.domain.store.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "매장 혜택 요청")
public class StoreBenefitRequest {

  @NotBlank
  @Schema(description = "혜택 설명", example = "군장병 10% 할인")
  private String description;

  @Min(0)
  @Max(100)
  @Schema(description = "할인율 (0~100)", example = "10")
  private Integer discountRate;

  @Schema(description = "혜택 조건", example = "군인 신분증 지참 시")
  private String conditionText;
}
