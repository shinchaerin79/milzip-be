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
      StoreCategory category,
      int page,
      int size,
      String sortBy,
      Double lat,
      Double lng,
      Double radiusKm,
      String keyword) {
    boolean hasKeyword = keyword != null && !keyword.isBlank();
    log.debug(
        "[StoreService] 매장 목록 조회 - category: {}, page: {}, size: {}, hasLocation: {}, radiusKm: {}, keyword: {}",
        category,
        page,
        size,
        lat != null && lng != null,
        radiusKm,
        keyword);

    if (lat != null && lng != null) {
      return getStoresSortedByDistance(category, page, size, lat, lng, radiusKm, keyword);
    }

    // 위치 없는 경우: 키워드/카테고리 없으면 DB 페이지네이션 최적화
    if (category == null && !hasKeyword) {
      Pageable pageable = buildPageable(page, size, sortBy);
      PageResponse<StoreListItemResponse> result =
          PageResponse.from(
              storeRepository.findAllWithBenefits(pageable).map(StoreListItemResponse::from));
      log.debug("[StoreService] 전체 매장 목록 조회 완료 - 총 {}건", result.getTotalElements());
      return result;
    }

    List<Store> candidates;
    if (category == null) {
      candidates = storeRepository.findByKeywordWithBenefitsList(keyword);
    } else {
      List<StoreCategory> dbCategories = StoreCategory.dbCategoriesFor(category);
      List<Store> raw =
          hasKeyword
              ? storeRepository.findByCategoriesAndKeywordWithBenefitsList(dbCategories, keyword)
              : storeRepository.findAllByCategoriesWithBenefitsList(dbCategories);
      candidates =
          raw.stream()
              .filter(s -> StoreCategory.resolve(s.getCategory(), s.getName()) == category)
              .toList();
    }

    List<Store> sorted =
        candidates.stream()
            .sorted(Comparator.comparingInt(Store::getViewCount).reversed())
            .toList();

    int start = page * size;
    int end = Math.min(start + size, sorted.size());
    List<Store> paged = start >= sorted.size() ? List.of() : sorted.subList(start, end);
    log.debug(
        "[StoreService] 매장 목록 조회 완료 - category: {}, keyword: {}, 총 {}건",
        category,
        keyword,
        sorted.size());
    return PageResponse.of(
        paged.stream().map(StoreListItemResponse::from).toList(), page, size, sorted.size());
  }

  @Transactional
  public StoreDetailResponse getStore(Long id) {
    log.debug("[StoreService] 매장 단건 조회 - storeId: {}", id);
    Store store =
        storeRepository
            .findWithBenefitsById(id)
            .orElseThrow(
                () -> {
                  log.warn("[StoreService] 매장 없음 - storeId: {}", id);
                  return new CustomException(StoreErrorCode.STORE_NOT_FOUND);
                });

    store.incrementViewCount();
    log.debug(
        "[StoreService] 매장 단건 조회 완료 - storeId: {}, name: {}, viewCount: {}",
        id,
        store.getName(),
        store.getViewCount());
    return StoreDetailResponse.from(store);
  }

  private PageResponse<StoreListItemResponse> getStoresSortedByDistance(
      StoreCategory category,
      int page,
      int size,
      double lat,
      double lng,
      Double radiusKm,
      String keyword) {
    boolean hasKeyword = keyword != null && !keyword.isBlank();

    List<Store> stores;
    if (category == null) {
      stores =
          hasKeyword
              ? storeRepository.findByKeywordWithLatLng(keyword)
              : storeRepository.findAllWithLatLng();
    } else {
      List<StoreCategory> dbCategories = StoreCategory.dbCategoriesFor(category);
      List<Store> raw =
          hasKeyword
              ? storeRepository.findByCategoriesAndKeywordWithLatLng(dbCategories, keyword)
              : storeRepository.findByCategoriesWithLatLng(dbCategories);
      stores =
          raw.stream()
              .filter(s -> StoreCategory.resolve(s.getCategory(), s.getName()) == category)
              .toList();
    }

    java.util.stream.Stream<StoreListItemResponse> stream =
        stores.stream()
            .map(
                s ->
                    StoreListItemResponse.from(
                        s,
                        GeoUtils.calculateDistanceKm(lat, lng, s.getLatitude(), s.getLongitude())));

    if (radiusKm != null) {
      stream = stream.filter(r -> r.getDistanceKm() <= radiusKm);
    }

    List<StoreListItemResponse> sorted =
        stream.sorted(Comparator.comparingDouble(StoreListItemResponse::getDistanceKm)).toList();

    int start = page * size;
    int end = Math.min(start + size, sorted.size());
    List<StoreListItemResponse> content =
        start >= sorted.size() ? List.of() : sorted.subList(start, end);

    log.debug(
        "[StoreService] 거리순 매장 목록 조회 완료 - category: {}, radiusKm: {}, 총 {}건",
        category,
        radiusKm,
        sorted.size());
    return PageResponse.of(content, page, size, sorted.size());
  }

  @Transactional(readOnly = true)
  public StoreDetailResponse getBestStore() {
    log.debug("[StoreService] 인기 매장(최다 조회수) 조회");
    List<Store> stores = storeRepository.findTopByViewCount(PageRequest.of(0, 1));
    if (stores.isEmpty()) {
      log.warn("[StoreService] 인기 매장 조회 실패 - 등록된 매장 없음");
      throw new CustomException(StoreErrorCode.STORE_NOT_FOUND);
    }
    log.debug(
        "[StoreService] 인기 매장 조회 완료 - storeId: {}, viewCount: {}",
        stores.get(0).getId(),
        stores.get(0).getViewCount());
    return StoreDetailResponse.from(stores.get(0));
  }

  @Transactional
  public StoreDetailResponse createStore(StoreCreateRequest request) {
    log.info(
        "[StoreService] 매장 등록 - name: {}, address: {}, category: {}",
        request.getName(),
        request.getAddress(),
        request.getCategory());

    if (storeRepository.existsByNameAndAddress(request.getName(), request.getAddress())) {
      log.warn(
          "[StoreService] 매장 등록 실패 - 이미 존재하는 매장 (name: {}, address: {})",
          request.getName(),
          request.getAddress());
      throw new CustomException(StoreErrorCode.STORE_ALREADY_EXISTS);
    }

    Store store = Store.create(request);
    storeRepository.save(store);

    if (request.getBenefits() != null) {
      for (StoreBenefitRequest benefitRequest : request.getBenefits()) {
        store.getBenefits().add(StoreBenefit.create(store, benefitRequest));
      }
    }

    log.info(
        "[StoreService] 매장 등록 완료 - storeId: {}, name: {}, 혜택: {}건",
        store.getId(),
        store.getName(),
        store.getBenefits().size());
    return StoreDetailResponse.from(store);
  }

  @Transactional
  public StoreDetailResponse updateStore(Long id, StoreCreateRequest request) {
    log.info("[StoreService] 매장 수정 - storeId: {}, name: {}", id, request.getName());

    Store store =
        storeRepository
            .findWithBenefitsById(id)
            .orElseThrow(
                () -> {
                  log.warn("[StoreService] 매장 수정 실패 - 매장 없음, storeId: {}", id);
                  return new CustomException(StoreErrorCode.STORE_NOT_FOUND);
                });

    store.update(request);
    store.getBenefits().clear();

    if (request.getBenefits() != null) {
      for (StoreBenefitRequest benefitRequest : request.getBenefits()) {
        store.getBenefits().add(StoreBenefit.create(store, benefitRequest));
      }
    }

    log.info("[StoreService] 매장 수정 완료 - storeId: {}, 혜택: {}건", id, store.getBenefits().size());
    return StoreDetailResponse.from(store);
  }

  @Transactional
  public void deleteStore(Long id) {
    log.info("[StoreService] 매장 삭제 - storeId: {}", id);
    Store store =
        storeRepository
            .findById(id)
            .orElseThrow(
                () -> {
                  log.warn("[StoreService] 매장 삭제 실패 - 매장 없음, storeId: {}", id);
                  return new CustomException(StoreErrorCode.STORE_NOT_FOUND);
                });
    storeRepository.delete(store);
    log.info("[StoreService] 매장 삭제 완료 - storeId: {}, name: {}", id, store.getName());
  }

  private Pageable buildPageable(int page, int size, String sortBy) {
    Sort sort = Sort.by(Sort.Direction.DESC, "viewCount");
    return PageRequest.of(page, size, sort);
  }
}
