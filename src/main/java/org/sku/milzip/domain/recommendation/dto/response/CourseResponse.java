package org.sku.milzip.domain.recommendation.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "코스 추천")
public class CourseResponse {

  @Schema(description = "코스 번호", example = "1")
  private int courseNumber;

  @Schema(description = "코스 지역 (시/도)", example = "경기도")
  private String region;

  @Schema(description = "코스 내 매장 목록")
  private List<AiRecommendationItemResponse> stores;
}
