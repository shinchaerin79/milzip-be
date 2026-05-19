package org.sku.milzip.domain.recommendation.controller;

import org.sku.milzip.domain.recommendation.dto.response.QuickRecommendationResponse;
import org.sku.milzip.domain.recommendation.service.RecommendationService;
import org.sku.milzip.domain.store.entity.StoreCategory;
import org.sku.milzip.global.common.BaseResponse;
import org.sku.milzip.global.common.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Recommendation", description = "추천 API")
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

  private final RecommendationService recommendationService;

  @Operation(
      summary = "[ 전체 | 토큰 X | 빠른 추천 매장 목록 조회 ]",
      description =
          """
          **Purpose**
          - 사용자 위치 기반으로 이동시간 1시간 이내의 군장병 혜택 매장을 추천합니다.

          **Query Parameters**
          - lat / lng: 현재 위치 (미입력 시 거리 필터링 없이 할인율순 반환)
          - category: FOOD / CAFE / LEISURE / ACCOMMODATION / ETC (미입력 시 전체)
          - sortBy: recommend (추천순, 기본값) / discount (할인율순)
          - page / size: 페이지네이션 (기본값 0 / 20)

          **추천순 점수 계산**
          - score = 할인율 × 0.6 + 근접도 × 0.4

          **이동시간 계산**
          - 도보 (4 km/h 기준): 이동시간 ≤ 60분이면 도보
          - 차량 (30 km/h 기준): 도보 초과 시 차량으로 재계산, 60분 초과 매장은 제외

          **Returns**
          - travelTimeMinutes: 예상 이동시간 (분)
          - travelMode: 도보 / 차량
          """)
  @GetMapping("/quick")
  public BaseResponse<PageResponse<QuickRecommendationResponse>> getQuickRecommendations(
      @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "페이지 크기", example = "20") @RequestParam(defaultValue = "20")
          int size,
      @Parameter(description = "카테고리 (FOOD / CAFE / LEISURE / ACCOMMODATION / ETC)")
          @RequestParam(required = false)
          StoreCategory category,
      @Parameter(description = "정렬 기준 (recommend: 추천순, discount: 할인율순)")
          @RequestParam(required = false)
          String sortBy,
      @Parameter(description = "위도 (거리 기반 필터링 시 필수)", example = "37.5665")
          @RequestParam(required = false)
          Double lat,
      @Parameter(description = "경도 (거리 기반 필터링 시 필수)", example = "126.9780")
          @RequestParam(required = false)
          Double lng) {
    return BaseResponse.success(
        recommendationService.getQuickRecommendations(category, page, size, sortBy, lat, lng));
  }
}
