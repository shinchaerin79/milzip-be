package org.sku.milzip.domain.benefit.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "영화관 혜택 응답")
public class MovieBenefitResponse {

  @Schema(description = "혜택 ID")
  private Long id;

  @Schema(description = "영화관 체인", example = "CGV")
  private String cinemaChain;

  @Schema(description = "혜택 설명", example = "일반(2D) 주중/주말 10,000원")
  private String description;
}
