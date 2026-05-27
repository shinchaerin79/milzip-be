package org.sku.milzip.domain.review.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "영수증 OCR 검증 결과")
public class ReceiptVerifyResponse {

  @Schema(description = "매장 일치 여부", example = "true")
  private boolean verified;

  @Schema(description = "OCR로 인식된 가게명", example = "삼겹살에 소주한잔")
  private String recognizedStoreName;

  @Schema(description = "검증 메시지", example = "영수증의 가게명이 일치합니다.")
  private String message;
}
