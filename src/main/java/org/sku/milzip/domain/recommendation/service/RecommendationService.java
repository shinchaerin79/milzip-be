package org.sku.milzip.domain.recommendation.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.sku.milzip.domain.recommendation.dto.request.AiRecommendationRequest;
import org.sku.milzip.domain.recommendation.dto.response.AiRecommendationItemResponse;
import org.sku.milzip.domain.recommendation.dto.response.AiRecommendationResponse;
import org.sku.milzip.domain.recommendation.dto.response.CourseResponse;
import org.sku.milzip.domain.recommendation.dto.response.QuickRecommendationResponse;
import org.sku.milzip.domain.recommendation.repository.VectorStoreRepository;
import org.sku.milzip.domain.store.entity.Store;
import org.sku.milzip.domain.store.entity.StoreCategory;
import org.sku.milzip.domain.store.repository.StoreRepository;
import org.sku.milzip.global.common.PageResponse;
import org.sku.milzip.global.kakao.KakaoMobilityService;
import org.sku.milzip.global.openai.OpenAiService;
import org.sku.milzip.global.openai.OpenAiService.AiRankResult;
import org.sku.milzip.global.util.GeoUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

  private static final double WALKING_SPEED_KMH = 4.0;
  private static final double DRIVING_SPEED_KMH = 40.0;
  private static final double ROAD_DISTANCE_FACTOR = 1.3; // 도보 직선거리 → 실제 도보거리 보정
  private static final double ROAD_DISTANCE_FACTOR_DRIVING =
      1.5; // 차량 직선거리 → 실제 도로거리 보정 (산악/도심 우회로 반영)
  private static final int MAX_WALKING_MINUTES = 30;
  private static final int MAX_DRIVING_MINUTES = 60;
  private static final int MAX_GPT_RECOMMENDATIONS = 10;

  private final StoreRepository storeRepository;
  private final VectorStoreRepository vectorStoreRepository;
  private final OpenAiService openAiService;
  private final KakaoMobilityService kakaoMobilityService;

  @Transactional(readOnly = true)
  public PageResponse<QuickRecommendationResponse> getQuickRecommendations(
      StoreCategory category, int page, int size, String sortBy, Double lat, Double lng) {

    List<Store> stores = fetchStores(category, lat, lng);

    List<QuickRecommendationResponse> result;

    if (lat != null && lng != null) {
      result =
          stores.stream()
              .map(s -> buildWithTravel(s, lat, lng))
              .filter(Objects::nonNull)
              .sorted(comparator(sortBy))
              .toList();
    } else {
      result =
          stores.stream()
              .map(QuickRecommendationResponse::from)
              .sorted(
                  Comparator.comparingInt(
                          (QuickRecommendationResponse r) ->
                              r.getMaxDiscountRate() != null ? r.getMaxDiscountRate() : 0)
                      .reversed())
              .toList();
    }

    int start = page * size;
    int end = Math.min(start + size, result.size());
    List<QuickRecommendationResponse> content =
        start >= result.size() ? List.of() : result.subList(start, end);

    return PageResponse.of(content, page, size, result.size());
  }

  private List<Store> fetchStores(StoreCategory category, Double lat, Double lng) {
    boolean withLatLng = lat != null && lng != null;
    if (category == null) {
      return withLatLng
          ? storeRepository.findAllWithLatLng()
          : storeRepository.findAllWithBenefitsList();
    }
    List<StoreCategory> dbCategories = StoreCategory.dbCategoriesFor(category);
    List<Store> raw =
        withLatLng
            ? storeRepository.findByCategoriesWithLatLng(dbCategories)
            : storeRepository.findAllByCategoriesWithBenefitsList(dbCategories);
    return raw.stream()
        .filter(s -> StoreCategory.resolve(s.getCategory(), s.getName()) == category)
        .toList();
  }

  private QuickRecommendationResponse buildWithTravel(Store store, double lat, double lng) {
    double distanceKm =
        GeoUtils.calculateDistanceKm(lat, lng, store.getLatitude(), store.getLongitude());

    double walkingMinutes = distanceKm * ROAD_DISTANCE_FACTOR / WALKING_SPEED_KMH * 60;
    double drivingMinutes = distanceKm * ROAD_DISTANCE_FACTOR_DRIVING / DRIVING_SPEED_KMH * 60;

    if (walkingMinutes <= MAX_WALKING_MINUTES) {
      return QuickRecommendationResponse.from(
          store, distanceKm, (int) Math.round(walkingMinutes), "도보");
    }
    if (drivingMinutes <= MAX_DRIVING_MINUTES) {
      return QuickRecommendationResponse.from(
          store, distanceKm, (int) Math.round(drivingMinutes), "차량");
    }
    return null;
  }

  // AI 맞춤 추천 (코스 형식)

  @Transactional(readOnly = true)
  public AiRecommendationResponse getAiRecommendations(AiRecommendationRequest request) {
    // 1. 쿼리 텍스트 구성 및 임베딩 생성
    String queryText = buildQueryText(request);
    log.debug("[RecommendationService] AI 추천 쿼리: {}", queryText);
    List<Float> embedding = openAiService.getEmbedding(queryText);

    // 2. 요청 카테고리 목록 (미입력 시 FOOD 기본)
    List<StoreCategory> requiredCategories =
        (request.getCategories() != null && !request.getCategories().isEmpty())
            ? request.getCategories()
            : List.of(StoreCategory.FOOD);

    // category -> 유사도순 후보 매장 (거리 필터 적용)
    Map<StoreCategory, List<Store>> candidatesByCategory = new LinkedHashMap<>();
    for (StoreCategory cat : requiredCategories) {
      List<Store> stores = vectorStoreRepository.findSimilarStoresByCategory(embedding, cat, 10);

      // lat/lng 제공 시: 차량 60분 초과 매장 제외 (좌표 없는 매장도 제외)
      if (request.getLat() != null && request.getLng() != null) {
        stores =
            stores.stream()
                .filter(s -> s.getLatitude() != null && s.getLongitude() != null)
                .filter(
                    s -> {
                      double distKm =
                          GeoUtils.calculateDistanceKm(
                              request.getLat(), request.getLng(),
                              s.getLatitude(), s.getLongitude());
                      return distKm / DRIVING_SPEED_KMH * 60 <= MAX_DRIVING_MINUTES;
                    })
                .toList();
      }

      if (!stores.isEmpty()) candidatesByCategory.put(cat, stores);
    }

    List<StoreCategory> missingCategories =
        requiredCategories.stream().filter(cat -> !candidatesByCategory.containsKey(cat)).toList();

    if (candidatesByCategory.isEmpty()) {
      log.info("[RecommendationService] AI 추천 후보 없음 (임베딩 미생성 또는 이동 가능 범위 내 매장 없음)");
      return AiRecommendationResponse.builder()
          .courses(List.of())
          .missingCategories(missingCategories)
          .build();
    }

    // 3. 거리 기반 코스 구성: 카테고리별 후보를 거리 오름차순 정렬 후
    //    1순위 매장들로 코스1, 2순위 매장들로 코스2, 3순위 매장들로 코스3
    //    좌표 없으면 유사도 순서 유지
    boolean hasLocation = request.getLat() != null && request.getLng() != null;

    Map<StoreCategory, List<Store>> rankedByCategory = new LinkedHashMap<>();
    for (Map.Entry<StoreCategory, List<Store>> entry : candidatesByCategory.entrySet()) {
      List<Store> sorted =
          hasLocation
              ? entry.getValue().stream()
                  .sorted(
                      Comparator.comparingDouble(
                          s ->
                              s.getLatitude() != null
                                  ? GeoUtils.calculateDistanceKm(
                                      request.getLat(), request.getLng(),
                                      s.getLatitude(), s.getLongitude())
                                  : Double.MAX_VALUE))
                  .toList()
              : entry.getValue();
      rankedByCategory.put(entry.getKey(), sorted);
    }

    int maxCourses =
        Math.min(3, rankedByCategory.values().stream().mapToInt(List::size).min().orElse(1));
    List<Map<StoreCategory, Store>> courseList = new ArrayList<>();
    for (int i = 0; i < maxCourses; i++) {
      Map<StoreCategory, Store> course = new LinkedHashMap<>();
      for (Map.Entry<StoreCategory, List<Store>> entry : rankedByCategory.entrySet()) {
        if (i < entry.getValue().size()) {
          course.put(entry.getKey(), entry.getValue().get(i));
        }
      }
      if (!course.isEmpty()) courseList.add(course);
    }

    // 4. GPT 추천 이유 생성 (선택된 매장 전체)
    List<Store> allSelected =
        courseList.stream().flatMap(c -> c.values().stream()).distinct().toList();

    String companionLabel =
        request.getCompanion() != null ? request.getCompanion().getLabel() : null;

    Map<Long, String> reasonMap;
    try {
      List<AiRankResult> rankResults =
          openAiService.rankStores(
              request.getFreeText(), companionLabel, null, allSelected, allSelected.size());
      reasonMap =
          rankResults.stream()
              .collect(Collectors.toMap(AiRankResult::storeId, AiRankResult::reason, (a, b) -> a));
    } catch (Exception e) {
      log.warn("[RecommendationService] GPT 이유 생성 실패, 기본값 사용", e);
      reasonMap = Map.of();
    }

    // 5. 코스 응답 구성
    List<CourseResponse> courses = new ArrayList<>();
    int courseNum = 1;
    for (Map<StoreCategory, Store> courseMap : courseList) {
      List<AiRecommendationItemResponse> items = new ArrayList<>();

      for (Store store : courseMap.values()) {
        Double distanceKm = null;
        Integer travelTimeMinutes = null;
        String travelMode = null;

        if (request.getLat() != null
            && request.getLng() != null
            && store.getLatitude() != null
            && store.getLongitude() != null) {

          double straightKm =
              GeoUtils.calculateDistanceKm(
                  request.getLat(), request.getLng(), store.getLatitude(), store.getLongitude());

          // 도보 가능 범위 (직선 × 1.3 보정)
          double walkingKm = straightKm * ROAD_DISTANCE_FACTOR;
          double walkMin = walkingKm / WALKING_SPEED_KMH * 60;

          if (walkMin <= MAX_WALKING_MINUTES) {
            distanceKm = walkingKm;
            travelTimeMinutes = (int) Math.round(walkMin);
            travelMode = "도보";
          } else {
            // 차량: Kakao Mobility API (실패 시 직선 거리 fallback)
            var route =
                kakaoMobilityService.getDrivingRoute(
                    request.getLat(), request.getLng(),
                    store.getLatitude(), store.getLongitude());

            if (route.isPresent()) {
              distanceKm = route.get().distanceKm();
              int driveSec = route.get().durationSeconds();
              travelTimeMinutes = (int) Math.round(driveSec / 60.0);
            } else {
              distanceKm = straightKm;
              travelTimeMinutes = (int) Math.round(straightKm / DRIVING_SPEED_KMH * 60);
            }
            travelMode = "차량";
          }
        }

        String reason = reasonMap.getOrDefault(store.getId(), "군장병 할인 혜택이 있는 매장입니다.");
        items.add(
            AiRecommendationItemResponse.of(
                store, reason, distanceKm, travelTimeMinutes, travelMode));
      }

      courses.add(CourseResponse.builder().courseNumber(courseNum++).stores(items).build());
    }
    return AiRecommendationResponse.builder()
        .courses(courses)
        .missingCategories(missingCategories)
        .build();
  }

  /** 주소에서 첫 번째 행정구역(시/도)을 추출합니다. */
  private String extractRegion(String address) {
    if (address == null || address.isBlank() || address.equalsIgnoreCase("nan")) {
      return "기타";
    }
    return address.trim().split(" ")[0];
  }

  private String buildQueryText(AiRecommendationRequest request) {
    StringBuilder sb = new StringBuilder(request.getFreeText());
    if (request.getCompanion() != null) {
      sb.append(" ").append(request.getCompanion().getLabel());
    }
    if (request.getCategories() != null && !request.getCategories().isEmpty()) {
      request.getCategories().forEach(c -> sb.append(" ").append(c.name()));
    }
    return sb.toString().trim();
  }

  private Comparator<QuickRecommendationResponse> comparator(String sortBy) {
    if ("distance".equals(sortBy)) {
      return Comparator.comparingDouble(
          r -> r.getDistanceKm() != null ? r.getDistanceKm() : Double.MAX_VALUE);
    }
    if ("discount".equals(sortBy)) {
      return Comparator.comparingInt(
              (QuickRecommendationResponse r) -> discountPercent(r.getMaxDiscountRate()))
          .reversed()
          .thenComparingDouble(
              r -> r.getDistanceKm() != null ? r.getDistanceKm() : Double.MAX_VALUE);
    }
    // 추천순: 퍼센트 할인율(0~100) 정규화 × 0.6 + 거리 근접도 정규화 × 0.4
    // - 할인율: 100원 초과(원 단위) 매장은 0으로 처리해 퍼센트 단위만 반영
    // - 근접도: 30 / (distanceKm + 0.5) → 가까울수록 최대 60, 멀수록 0에 수렴
    return Comparator.comparingDouble(
            (QuickRecommendationResponse r) -> {
              double discountScore = discountPercent(r.getMaxDiscountRate());
              double proximityScore =
                  r.getDistanceKm() != null ? 30.0 / (r.getDistanceKm() + 0.5) : 0;
              return discountScore * 0.6 + proximityScore * 0.4;
            })
        .reversed();
  }

  /** 할인율이 퍼센트(0~100) 단위일 때만 반환하고, 100 초과(원 단위)이면 0으로 처리합니다. */
  private int discountPercent(Integer maxDiscountRate) {
    if (maxDiscountRate == null || maxDiscountRate > 100) return 0;
    return maxDiscountRate;
  }
}
