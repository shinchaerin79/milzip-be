package org.sku.milzip.domain.review.dto.response;

import java.util.Map;

import org.sku.milzip.domain.review.entity.GoodPoint;
import org.sku.milzip.global.common.PageResponse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "매장 리뷰 목록 응답")
public class ReviewListResponse {

  @Schema(description = "리뷰 목록 (페이지네이션)")
  private PageResponse<ReviewResponse> reviews;

  @Schema(description = "좋았던 점 항목별 집계 (해당 매장의 전체 리뷰 기준)")
  private Map<GoodPoint, Long> goodPointCounts;
}
