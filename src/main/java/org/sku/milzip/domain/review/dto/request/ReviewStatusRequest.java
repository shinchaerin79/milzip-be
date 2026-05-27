package org.sku.milzip.domain.review.dto.request;

import jakarta.validation.constraints.NotNull;

import org.sku.milzip.domain.review.entity.ReviewStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "리뷰 상태 변경 요청 (관리자 전용)")
public class ReviewStatusRequest {

  @Schema(description = "변경할 상태 (VISIBLE / HIDDEN)", example = "HIDDEN")
  @NotNull(message = "변경할 상태를 입력해주세요.") private ReviewStatus status;
}
