package org.sku.milzip.domain.store.dto.response;

import org.sku.milzip.domain.store.entity.StoreBenefit;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "매장 혜택 정보")
public class StoreBenefitResponse {

  @Schema(description = "혜택 ID", example = "1")
  private Long id;

  @Schema(description = "혜택 내용", example = "이용금액의 10% 할인")
  private String description;

  @Schema(description = "할인율 (%)", example = "10")
  private Integer discountRate;

  @Schema(description = "이용 조건", example = "카드 이용 시 5% 할인")
  private String conditionText;

  public static StoreBenefitResponse from(StoreBenefit benefit) {
    return new StoreBenefitResponse(
        benefit.getId(),
        benefit.getDescription(),
        benefit.getDiscountRate(),
        benefit.getConditionText());
  }
}
