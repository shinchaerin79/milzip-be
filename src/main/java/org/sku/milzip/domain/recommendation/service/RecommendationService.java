package org.sku.milzip.domain.recommendation.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.sku.milzip.domain.recommendation.dto.response.QuickRecommendationResponse;
import org.sku.milzip.domain.store.entity.Store;
import org.sku.milzip.domain.store.entity.StoreCategory;
import org.sku.milzip.domain.store.repository.StoreRepository;
import org.sku.milzip.global.common.PageResponse;
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
  private static final double DRIVING_SPEED_KMH = 30.0;
  private static final int MAX_TRAVEL_MINUTES = 60;

  private final StoreRepository storeRepository;

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
    if (withLatLng) {
      return category == null
          ? storeRepository.findMilitaryBenefitWithLatLng()
          : storeRepository.findMilitaryBenefitByCategoryWithLatLng(category);
    }
    return category == null
        ? storeRepository.findAllMilitaryBenefit()
        : storeRepository.findMilitaryBenefitByCategory(category);
  }

  private QuickRecommendationResponse buildWithTravel(Store store, double lat, double lng) {
    double distanceKm =
        GeoUtils.calculateDistanceKm(lat, lng, store.getLatitude(), store.getLongitude());

    double walkingMinutes = distanceKm / WALKING_SPEED_KMH * 60;
    double drivingMinutes = distanceKm / DRIVING_SPEED_KMH * 60;

    if (walkingMinutes <= MAX_TRAVEL_MINUTES) {
      return QuickRecommendationResponse.from(
          store, distanceKm, (int) Math.round(walkingMinutes), "도보");
    }
    if (drivingMinutes <= MAX_TRAVEL_MINUTES) {
      return QuickRecommendationResponse.from(
          store, distanceKm, (int) Math.round(drivingMinutes), "차량");
    }
    return null;
  }

  private Comparator<QuickRecommendationResponse> comparator(String sortBy) {
    if ("discount".equals(sortBy)) {
      return Comparator.comparingInt(
              (QuickRecommendationResponse r) ->
                  r.getMaxDiscountRate() != null ? r.getMaxDiscountRate() : 0)
          .reversed()
          .thenComparingDouble(r -> r.getDistanceKm() != null ? r.getDistanceKm() : 0);
    }
    // 추천순: 할인율 60% + 거리 근접도 40% 가중 점수
    return Comparator.comparingDouble(
            (QuickRecommendationResponse r) -> {
              double discountScore = r.getMaxDiscountRate() != null ? r.getMaxDiscountRate() : 0;
              double proximityScore =
                  r.getDistanceKm() != null ? 30.0 / (r.getDistanceKm() + 0.5) : 0;
              return discountScore * 0.6 + proximityScore * 0.4;
            })
        .reversed();
  }
}
