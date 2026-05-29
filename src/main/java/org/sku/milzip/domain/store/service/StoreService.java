package org.sku.milzip.domain.store.service;

import java.util.Comparator;
import java.util.List;

import org.sku.milzip.domain.store.dto.request.StoreBenefitRequest;
import org.sku.milzip.domain.store.dto.request.StoreCreateRequest;
import org.sku.milzip.domain.store.dto.response.StoreDetailResponse;
import org.sku.milzip.domain.store.dto.response.StoreListItemResponse;
import org.sku.milzip.domain.store.entity.Store;
import org.sku.milzip.domain.store.entity.StoreBenefit;
import org.sku.milzip.domain.store.entity.StoreCategory;
import org.sku.milzip.domain.store.exception.StoreErrorCode;
import org.sku.milzip.domain.store.repository.StoreRepository;
import org.sku.milzip.global.common.PageResponse;
import org.sku.milzip.global.exception.CustomException;
import org.sku.milzip.global.util.GeoUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreService {

  private final StoreRepository storeRepository;

  @Transactional(readOnly = true)
  public PageResponse<StoreListItemResponse> getStores(
      StoreCategory category, int page, int size, String sortBy, Double lat, Double lng) {

    // lat/lng가 있으면 sortBy 값에 관계없이 거리순으로 정렬
    if (lat != null && lng != null) {
      return getStoresSortedByDistance(category, page, size, lat, lng);
    }

    Pageable pageable = buildPageable(page, size, sortBy);

    Page<Store> stores =
        category == null
            ? storeRepository.findAllWithBenefits(pageable)
            : storeRepository.findByCategoryWithBenefits(category, pageable);

    return PageResponse.from(stores.map(StoreListItemResponse::from));
  }

  @Transactional
  public StoreDetailResponse getStore(Long id) {
    Store store =
        storeRepository
            .findWithBenefitsById(id)
            .orElseThrow(() -> new CustomException(StoreErrorCode.STORE_NOT_FOUND));

    store.incrementViewCount();
    return StoreDetailResponse.from(store);
  }

  private PageResponse<StoreListItemResponse> getStoresSortedByDistance(
      StoreCategory category, int page, int size, double lat, double lng) {

    List<Store> stores =
        category == null
            ? storeRepository.findAllWithLatLng()
            : storeRepository.findByCategoryWithLatLng(category);

    List<StoreListItemResponse> sorted =
        stores.stream()
            .map(
                s ->
                    StoreListItemResponse.from(
                        s,
                        GeoUtils.calculateDistanceKm(lat, lng, s.getLatitude(), s.getLongitude())))
            .sorted(Comparator.comparingDouble(r -> r.getDistanceKm()))
            .toList();

    int start = page * size;
    int end = Math.min(start + size, sorted.size());
    List<StoreListItemResponse> content =
        start >= sorted.size() ? List.of() : sorted.subList(start, end);

    return PageResponse.of(content, page, size, sorted.size());
  }

  @Transactional(readOnly = true)
  public StoreDetailResponse getBestStore() {
    List<Store> stores = storeRepository.findTopByViewCount(PageRequest.of(0, 1));
    if (stores.isEmpty()) {
      throw new CustomException(StoreErrorCode.STORE_NOT_FOUND);
    }
    return StoreDetailResponse.from(stores.get(0));
  }

  @Transactional
  public StoreDetailResponse createStore(StoreCreateRequest request) {
    if (storeRepository.existsByNameAndAddress(request.getName(), request.getAddress())) {
      throw new CustomException(StoreErrorCode.STORE_ALREADY_EXISTS);
    }

    Store store = Store.create(request);
    storeRepository.save(store);

    if (request.getBenefits() != null) {
      for (StoreBenefitRequest benefitRequest : request.getBenefits()) {
        store.getBenefits().add(StoreBenefit.create(store, benefitRequest));
      }
    }

    return StoreDetailResponse.from(store);
  }

  @Transactional
  public StoreDetailResponse updateStore(Long id, StoreCreateRequest request) {
    Store store =
        storeRepository
            .findWithBenefitsById(id)
            .orElseThrow(() -> new CustomException(StoreErrorCode.STORE_NOT_FOUND));

    store.update(request);
    store.getBenefits().clear();

    if (request.getBenefits() != null) {
      for (StoreBenefitRequest benefitRequest : request.getBenefits()) {
        store.getBenefits().add(StoreBenefit.create(store, benefitRequest));
      }
    }

    return StoreDetailResponse.from(store);
  }

  @Transactional
  public void deleteStore(Long id) {
    Store store =
        storeRepository
            .findById(id)
            .orElseThrow(() -> new CustomException(StoreErrorCode.STORE_NOT_FOUND));
    storeRepository.delete(store);
  }

  private Pageable buildPageable(int page, int size, String sortBy) {
    Sort sort = Sort.by(Sort.Direction.DESC, "viewCount");
    return PageRequest.of(page, size, sort);
  }
}
