package org.sku.milzip.domain.recommendation.service;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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
import org.sku.milzip.global.kakao.KakaoMobilityService.RouteResult;
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
  private static final double ROAD_DISTANCE_FACTOR = 1.3;
  private static final double ROAD_DISTANCE_FACTOR_DRIVING = 1.5;
  private static final int MAX_WALKING_MINUTES = 30;
  private static final int MAX_DRIVING_MINUTES = 60;

  private final StoreRepository storeRepository;
  private final VectorStoreRepository vectorStoreRepository;
  private final OpenAiService openAiService;
  private final KakaoMobilityService kakaoMobilityService;

  @Transactional(readOnly = true)
  public PageResponse<QuickRecommendationResponse> getQuickRecommendations(
      StoreCategory category, int page, int size, String sortBy, Double lat, Double lng) {
    log.debug(
        "[RecommendationService] 빠른 추천 조회 - category: {}, page: {}, size: {}, sortBy: {}, hasLocation: {}",
        category,
        page,
        size,
        sortBy,
        lat != null && lng != null);

    List<Store> stores = fetchStores(category, lat, lng);
    log.debug("[RecommendationService] 빠른 추천 후보 매장 수: {}", stores.size());

    List<QuickRecommendationResponse> result;

    if (lat != null && lng != null) {
      result =
          stores.stream()
              .map(s -> buildWithTravel(s, lat, lng))
              .filter(Objects::nonNull)
              .sorted(comparator(sortBy))
              .toList();
      log.debug("[RecommendationService] 거리 필터링 후 매장 수: {} → {}", stores.size(), result.size());
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

    log.debug(
        "[RecommendationService] 빠른 추천 완료 - 결과 총 {}건, 현재 페이지 {}건", result.size(), content.size());
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
    log.info(
        "[RecommendationService] AI 추천 요청 - categories: {}, companion: {}, hasLocation: {}, freeText: {}",
        request.getCategories(),
        request.getCompanion(),
        request.getLat() != null && request.getLng() != null,
        request.getFreeText());

    // 1. 쿼리 텍스트 구성 및 임베딩 생성
    String queryText = buildQueryText(request);
    log.debug("[RecommendationService] AI 추천 쿼리: {}", queryText);
    List<Float> embedding = openAiService.getEmbedding(queryText);

    // 2. 요청 카테고리 목록 (미입력 시 FOOD 기본)
    List<StoreCategory> requiredCategories =
        (request.getCategories() != null && !request.getCategories().isEmpty())
            ? request.getCategories()
            : List.of(StoreCategory.FOOD);

    // 3. 카테고리별 벡터 유사도 검색 + 거리 필터 — 병렬 실행
    List<CompletableFuture<AbstractMap.SimpleEntry<StoreCategory, List<Store>>>> futures =
        requiredCategories.stream()
            .map(
                cat ->
                    CompletableFuture.supplyAsync(
                        () -> {
                          List<Store> stores =
                              vectorStoreRepository.findSimilarStoresByCategory(embedding, cat, 10);
                          if (request.getLat() != null && request.getLng() != null) {
                            stores =
                                stores.stream()
                                    .filter(
                                        s -> s.getLatitude() != null && s.getLongitude() != null)
                                    .filter(
                                        s -> {
                                          double distKm =
                                              GeoUtils.calculateDistanceKm(
                                                  request.getLat(), request.getLng(),
                                                  s.getLatitude(), s.getLongitude());
                                          return distKm / DRIVING_SPEED_KMH * 60
                                              <= MAX_DRIVING_MINUTES;
                                        })
                                    .toList();
                          }
                          return new AbstractMap.SimpleEntry<>(cat, stores);
                        }))
            .toList();

    Map<StoreCategory, List<Store>> candidatesByCategory = new LinkedHashMap<>();
    for (var future : futures) {
      var entry = future.join();
      if (!entry.getValue().isEmpty()) {
        candidatesByCategory.put(entry.getKey(), entry.getValue());
      }
    }

    List<StoreCategory> missingCategories =
        requiredCategories.stream().filter(cat -> !candidatesByCategory.containsKey(cat)).toList();

    for (Map.Entry<StoreCategory, List<Store>> entry : candidatesByCategory.entrySet()) {
      log.debug(
          "[RecommendationService] 카테고리별 후보 수 - category: {}, count: {}",
          entry.getKey(),
          entry.getValue().size());
    }
    if (!missingCategories.isEmpty()) {
      log.info("[RecommendationService] 후보 없는 카테고리 - {}", missingCategories);
    }

    if (candidatesByCategory.isEmpty()) {
      log.info("[RecommendationService] AI 추천 후보 없음 (임베딩 미생성 또는 이동 가능 범위 내 매장 없음)");
      return AiRecommendationResponse.builder()
          .courses(List.of())
          .missingCategories(missingCategories)
          .build();
    }

    // 4. 코스 구성
    boolean hasLocation = request.getLat() != null && request.getLng() != null;

    Map<StoreCategory, List<Store>> rankedByCategory = new LinkedHashMap<>();
    for (Map.Entry<StoreCategory, List<Store>> entry : candidatesByCategory.entrySet()) {
      List<Store> candidates = entry.getValue();
      List<Store> sorted;
      if (hasLocation && candidates.size() > 1) {
        int n = candidates.size();

        Map<Store, Double> simScore = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
          simScore.put(candidates.get(i), n == 1 ? 1.0 : (double) (n - 1 - i) / (n - 1));
        }

        List<Store> byDist =
            candidates.stream()
                .sorted(
                    Comparator.comparingDouble(
                        s ->
                            s.getLatitude() != null
                                ? GeoUtils.calculateDistanceKm(
                                    request.getLat(), request.getLng(),
                                    s.getLatitude(), s.getLongitude())
                                : Double.MAX_VALUE))
                .toList();
        Map<Store, Double> distScore = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
          distScore.put(byDist.get(i), n == 1 ? 1.0 : (double) (n - 1 - i) / (n - 1));
        }

        sorted =
            candidates.stream()
                .sorted(
                    Comparator.comparingDouble(
                            (Store s) ->
                                simScore.getOrDefault(s, 0.0) * 0.6
                                    + distScore.getOrDefault(s, 0.0) * 0.4)
                        .reversed())
                .toList();
      } else {
        sorted = candidates;
      }
      rankedByCategory.put(entry.getKey(), sorted);
    }

    List<Map<StoreCategory, Store>> courseList;
    if (hasLocation && rankedByCategory.size() > 1) {
      courseList = buildCoursesWithAnchor(rankedByCategory);
    } else {
      int maxCourses =
          Math.min(3, rankedByCategory.values().stream().mapToInt(List::size).min().orElse(1));
      courseList = new ArrayList<>();
      for (int i = 0; i < maxCourses; i++) {
        Map<StoreCategory, Store> course = new LinkedHashMap<>();
        for (Map.Entry<StoreCategory, List<Store>> entry : rankedByCategory.entrySet()) {
          if (i < entry.getValue().size()) {
            course.put(entry.getKey(), entry.getValue().get(i));
          }
        }
        if (!course.isEmpty()) courseList.add(course);
      }
    }

    log.info(
        "[RecommendationService] GPT 호출 전 코스별 슬롯 - {}",
        courseList.stream().map(c -> c.keySet().toString()).collect(Collectors.joining(" | ")));

    // 5. GPT 추천 이유 생성 (선택된 매장 전체)
    List<Store> allSelected =
        courseList.stream().flatMap(c -> c.values().stream()).distinct().toList();

    String companionLabel =
        request.getCompanion() != null ? request.getCompanion().getLabel() : null;

    Map<Long, String> reasonMap;
    try {
      List<AiRankResult> rankResults =
          openAiService.generateReasons(
              request.getFreeText(), companionLabel, requiredCategories, allSelected);
      reasonMap =
          rankResults.stream()
              .collect(Collectors.toMap(AiRankResult::storeId, AiRankResult::reason, (a, b) -> a));
    } catch (Exception e) {
      log.warn("[RecommendationService] GPT 이유 생성 실패, 기본값 사용", e);
      reasonMap = Map.of();
    }

    // 6. GPT가 의도에 맞지 않아 제외한 매장을 코스에서 제거
    // 코스 슬롯 키가 FOOD인 매장에만 GPT 필터 적용 — 사용자가 명시적으로 선택한 CAFE 등 다른 카테고리 슬롯은 항상 유지
    final Map<Long, String> finalReasonMap = reasonMap;
    final boolean foodRequested = requiredCategories.contains(StoreCategory.FOOD);
    if (!finalReasonMap.isEmpty()) {
      courseList =
          courseList.stream()
              .map(
                  courseMap -> {
                    Map<StoreCategory, Store> filtered = new LinkedHashMap<>(courseMap);
                    filtered
                        .entrySet()
                        .removeIf(
                            e -> {
                              // 코스 슬롯 카테고리 키 기준 — FOOD 슬롯만 GPT 필터 적용
                              if (e.getKey() == StoreCategory.FOOD) {
                                return !finalReasonMap.containsKey(e.getValue().getId());
                              }
                              return false;
                            });
                    return filtered;
                  })
              // 음식이 요청된 코스인데 음식점이 빠져 카페 등만 남은 코스는 제거 (앵커 없는 코스 방지)
              .filter(
                  courseMap ->
                      !courseMap.isEmpty()
                          && (!foodRequested || courseMap.containsKey(StoreCategory.FOOD)))
              .collect(Collectors.toList());
    }
    log.info(
        "[RecommendationService] GPT 필터링 후 코스별 매장 - {}",
        courseList.stream().map(c -> c.keySet().toString()).collect(Collectors.joining(" | ")));

    // 카카오 경로 조회 대상은 필터링 후 코스에 남은 매장만 사용
    List<Store> storesForRouting =
        courseList.stream().flatMap(c -> c.values().stream()).distinct().toList();

    // 7. 카카오 모빌리티 경로 병렬 조회
    Map<Long, Optional<RouteResult>> drivingRoutes = new ConcurrentHashMap<>();
    if (hasLocation) {
      List<CompletableFuture<Void>> routeFutures =
          storesForRouting.stream()
              .filter(s -> s.getLatitude() != null && s.getLongitude() != null)
              .filter(
                  s -> {
                    double km =
                        GeoUtils.calculateDistanceKm(
                            request.getLat(), request.getLng(),
                            s.getLatitude(), s.getLongitude());
                    return km * ROAD_DISTANCE_FACTOR / WALKING_SPEED_KMH * 60 > MAX_WALKING_MINUTES;
                  })
              .map(
                  s ->
                      CompletableFuture.supplyAsync(
                              () ->
                                  kakaoMobilityService.getDrivingRoute(
                                      request.getLat(), request.getLng(),
                                      s.getLatitude(), s.getLongitude()))
                          .thenAccept(r -> drivingRoutes.put(s.getId(), r)))
              .toList();
      CompletableFuture.allOf(routeFutures.toArray(new CompletableFuture[0])).join();
    }

    // 8. 코스 응답 구성
    List<CourseResponse> courses = new ArrayList<>();
    int courseNum = 1;
    for (Map<StoreCategory, Store> courseMap : courseList) {
      List<AiRecommendationItemResponse> items = new ArrayList<>();

      // 코스의 앵커(FOOD 슬롯, 없으면 첫 번째) — 비음식 매장 추천 이유에 활용
      Store anchorStore =
          courseMap.containsKey(StoreCategory.FOOD)
              ? courseMap.get(StoreCategory.FOOD)
              : courseMap.values().iterator().next();

      for (Map.Entry<StoreCategory, Store> entry : courseMap.entrySet()) {
        StoreCategory slot = entry.getKey();
        Store store = entry.getValue();
        Double distanceKm = null;
        Integer travelTimeMinutes = null;
        String travelMode = null;

        if (hasLocation && store.getLatitude() != null && store.getLongitude() != null) {
          double straightKm =
              GeoUtils.calculateDistanceKm(
                  request.getLat(), request.getLng(), store.getLatitude(), store.getLongitude());
          double walkingKm = straightKm * ROAD_DISTANCE_FACTOR;
          double walkMin = walkingKm / WALKING_SPEED_KMH * 60;

          if (walkMin <= MAX_WALKING_MINUTES) {
            distanceKm = walkingKm;
            travelTimeMinutes = (int) Math.round(walkMin);
            travelMode = "도보";
          } else {
            Optional<RouteResult> route =
                drivingRoutes.getOrDefault(store.getId(), Optional.empty());
            if (route.isPresent()) {
              distanceKm = route.get().distanceKm();
              travelTimeMinutes = (int) Math.round(route.get().durationSeconds() / 60.0);
            } else {
              distanceKm = straightKm;
              travelTimeMinutes = (int) Math.round(straightKm / DRIVING_SPEED_KMH * 60);
            }
            travelMode = "차량";
          }
        }

        String reason = buildItemReason(reasonMap, slot, store, anchorStore);
        items.add(
            AiRecommendationItemResponse.of(
                store, reason, distanceKm, travelTimeMinutes, travelMode));
      }

      courses.add(CourseResponse.builder().courseNumber(courseNum++).stores(items).build());
    }
    log.info(
        "[RecommendationService] AI 추천 완료 - 코스 {}개 구성, 누락 카테고리: {}",
        courses.size(),
        missingCategories);
    return AiRecommendationResponse.builder()
        .courses(courses)
        .missingCategories(missingCategories)
        .build();
  }

  /**
   * 매장 추천 이유를 결정합니다. GPT가 생성한 이유가 있으면 그것을 사용하고, 없으면(주로 카페 등 비음식 매장) 코스의 음식점과의 거리·관계를 담은 이유를 생성합니다.
   */
  private String buildItemReason(
      Map<Long, String> reasonMap, StoreCategory slot, Store store, Store anchorStore) {
    String gptReason = reasonMap.get(store.getId());
    if (gptReason != null) {
      return gptReason;
    }

    String discountPhrase = buildDiscountPhrase(store);

    // 앵커(음식점)와 다른 매장이고 좌표가 있으면 거리 기반 이유 생성
    if (store != anchorStore
        && anchorStore.getLatitude() != null
        && anchorStore.getLongitude() != null
        && store.getLatitude() != null
        && store.getLongitude() != null) {
      double km =
          GeoUtils.calculateDistanceKm(
              anchorStore.getLatitude(), anchorStore.getLongitude(),
              store.getLatitude(), store.getLongitude());
      return String.format(
          "코스 내 %s에서 약 %.1fkm 거리로, 식사 후 들르기 좋은 %s입니다.%s",
          anchorStore.getName(), km, slot.getLabel(), discountPhrase);
    }

    // 단독 매장이면 카테고리 + 할인 정보로 이유 구성
    return String.format("군장병 혜택이 있는 %s입니다.%s", slot.getLabel(), discountPhrase);
  }

  /** 매장 할인 정보를 자연어 구절로 변환합니다. (예: " 군장병 10% 할인 혜택을 받을 수 있습니다.") */
  private String buildDiscountPhrase(Store store) {
    String discountInfo =
        store.getBenefits().stream()
            .map(b -> b.getDescription())
            .filter(d -> d != null && !d.isBlank())
            .findFirst()
            .orElse(null);
    Integer maxDiscount =
        store.getBenefits().stream()
            .map(b -> b.getDiscountRate())
            .filter(Objects::nonNull)
            .filter(d -> d > 0 && d <= 100)
            .max(Integer::compareTo)
            .orElse(null);

    if (maxDiscount != null) {
      return String.format(" 군장병 %d%% 할인 혜택을 받을 수 있습니다.", maxDiscount);
    }
    if (discountInfo != null) {
      return String.format(" 군장병 혜택: %s.", discountInfo);
    }
    return "";
  }

  private List<Map<StoreCategory, Store>> buildCoursesWithAnchor(
      Map<StoreCategory, List<Store>> rankedByCategory) {

    StoreCategory anchorCategory =
        rankedByCategory.containsKey(StoreCategory.FOOD)
            ? StoreCategory.FOOD
            : rankedByCategory.keySet().iterator().next();

    List<Store> anchors = rankedByCategory.get(anchorCategory);
    List<StoreCategory> companions =
        rankedByCategory.keySet().stream().filter(c -> c != anchorCategory).toList();

    // 미사용 풀(중복 방지용) + 전체 목록(풀 소진 시 재사용용)
    Map<StoreCategory, List<Store>> pool = new LinkedHashMap<>();
    companions.forEach(c -> pool.put(c, new ArrayList<>(rankedByCategory.get(c))));

    List<Map<StoreCategory, Store>> courses = new ArrayList<>();
    int maxCourses = Math.min(3, anchors.size());

    for (int i = 0; i < maxCourses; i++) {
      Store anchor = anchors.get(i);
      Map<StoreCategory, Store> course = new LinkedHashMap<>();
      course.put(anchorCategory, anchor);

      for (StoreCategory cat : companions) {
        List<Store> available = pool.get(cat);
        // 미사용 후보가 남아있으면 그 안에서, 모두 소진됐으면 전체 목록에서 선택(재사용 허용)
        boolean reuse = available.isEmpty();
        List<Store> source = reuse ? rankedByCategory.get(cat) : available;
        if (source.isEmpty()) continue;

        Store nearest = pickNearest(source, anchor);
        course.put(cat, nearest);
        if (!reuse) available.remove(nearest);
      }

      courses.add(course);
    }

    return courses;
  }

  /** 앵커 매장에서 가장 가까운 매장을 선택합니다. 앵커 좌표가 없으면 목록의 첫 번째(유사도 1위)를 반환합니다. */
  private Store pickNearest(List<Store> candidates, Store anchor) {
    if (anchor.getLatitude() == null || anchor.getLongitude() == null) {
      return candidates.get(0);
    }
    return candidates.stream()
        .filter(s -> s.getLatitude() != null && s.getLongitude() != null)
        .min(
            Comparator.comparingDouble(
                s ->
                    GeoUtils.calculateDistanceKm(
                        anchor.getLatitude(), anchor.getLongitude(),
                        s.getLatitude(), s.getLongitude())))
        .orElse(candidates.get(0));
  }

  private String buildQueryText(AiRecommendationRequest request) {
    StringBuilder sb = new StringBuilder(request.getFreeText());
    if (request.getCompanion() != null) {
      sb.append(" ").append(request.getCompanion().getLabel());
    }
    if (request.getCategories() != null && !request.getCategories().isEmpty()) {
      // 한국어 레이블 사용 — 임베딩 모델의 한국어 유사도 매칭에 유리
      request.getCategories().forEach(c -> sb.append(" ").append(c.getLabel()));
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
    return Comparator.comparingDouble(
            (QuickRecommendationResponse r) -> {
              double discountScore = discountPercent(r.getMaxDiscountRate());
              double proximityScore =
                  r.getDistanceKm() != null ? 30.0 / (r.getDistanceKm() + 0.5) : 0;
              return discountScore * 0.6 + proximityScore * 0.4;
            })
        .reversed();
  }

  private int discountPercent(Integer maxDiscountRate) {
    if (maxDiscountRate == null || maxDiscountRate > 100) return 0;
    return maxDiscountRate;
  }
}
