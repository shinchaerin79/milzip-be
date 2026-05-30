package org.sku.milzip.domain.recommendation.dto.response;

import java.util.List;

import org.sku.milzip.domain.store.entity.StoreCategory;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "AI 맞춤 추천 응답")
public class AiRecommendationResponse {

  @Schema(description = "추천 코스 목록")
  private List<CourseResponse> courses;

  @Schema(description = "거리 내 매장이 없어 코스에서 제외된 카테고리", example = "[\"CAFE\"]")
  private List<StoreCategory> missingCategories;
}
