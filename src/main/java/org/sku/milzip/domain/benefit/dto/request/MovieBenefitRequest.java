package org.sku.milzip.domain.benefit.dto.request;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "영화 혜택 등록/수정 요청")
public class MovieBenefitRequest {

  @NotBlank
  @Schema(description = "영화관 체인", example = "CGV")
  private String cinemaChain;

  @NotBlank
  @Schema(description = "혜택 설명", example = "일반(2D) 주중/주말 10,000원")
  private String description;
}
