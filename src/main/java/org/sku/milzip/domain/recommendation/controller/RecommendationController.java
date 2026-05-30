package org.sku.milzip.domain.recommendation.controller;

import jakarta.validation.Valid;

import org.sku.milzip.domain.recommendation.dto.request.AiRecommendationRequest;
import org.sku.milzip.domain.recommendation.dto.response.AiRecommendationResponse;
import org.sku.milzip.domain.recommendation.dto.response.QuickRecommendationResponse;
import org.sku.milzip.domain.recommendation.service.RecommendationService;
import org.sku.milzip.domain.store.entity.StoreCategory;
import org.sku.milzip.global.common.BaseResponse;
import org.sku.milzip.global.common.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

  // POST + @RequestBody 사용 이유:
  //   - freeText가 자유 입력 텍스트라 URL 파라미터(GET)로 전달 시 길이 제한·특수문자 인코딩 문제 발생
  //   - categories 배열 및 enum 필드는 Swagger JSON body에서 드롭다운으로 자동 표시됨
  //   - AI 추론 요청 특성상 POST 의미론적으로 적합 (서버 상태 변경은 없으나 계산 비용이 큰 요청)
  @Operation(
      summary = "[ 전체 | 토큰 X | AI 맞춤 추천 코스 조회 ]",
      description =
          """
          **Purpose**
          - 동행자·카테고리·자유 텍스트를 기반으로 AI가 같은 지역 매장을 묶어 코스로 추천합니다.

          **동작 방식**
          1. 입력값을 OpenAI 임베딩으로 변환
          2. 선택한 카테고리별로 pgvector 코사인 유사도 검색 (카테고리당 최대 10개)
          3. lat/lng 입력 시 차량 60분 초과 매장 제외
          4. GPT-4o-mini가 추천 이유 생성
          5. 시/도 기준으로 그룹핑 → 코스 1, 코스 2 형식으로 반환

          **Request Body (JSON)**
          - freeText (필수): 자유 텍스트 요청 (예: "삼겹살 먹고 싶어")
          - companion (선택): FRIEND / COUPLE / FAMILY / ALONE
          - categories (선택): 방문할 카테고리 1~3개 선택
            · FOOD (식사) / CAFE (카페) / LEISURE (여가) / ACCOMMODATION (숙박) / ETC (기타)
            · 미입력 시 FOOD 기본 적용
          - lat / lng (선택): 현재 위치 (입력 시 거리·이동수단 포함, 차량 60분 초과 매장 제외)

          **Returns**
          - courseNumber: 코스 번호 (1, 2, ...)
          - region: 해당 코스의 지역 (시/도)
          - stores: 코스 내 매장 목록 (reason 포함)

          **StoreCategory 위치**
          - `domain/store/entity/StoreCategory.java`
          """)
  @PostMapping("/ai")
  public BaseResponse<AiRecommendationResponse> getAiRecommendations(
      @Valid @RequestBody AiRecommendationRequest request) {
    return BaseResponse.success(recommendationService.getAiRecommendations(request));
  }
}
