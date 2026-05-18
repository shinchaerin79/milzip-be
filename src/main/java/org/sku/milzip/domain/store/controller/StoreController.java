package org.sku.milzip.domain.store.controller;

import org.sku.milzip.domain.store.dto.response.StoreDetailResponse;
import org.sku.milzip.domain.store.dto.response.StoreListItemResponse;
import org.sku.milzip.domain.store.entity.StoreCategory;
import org.sku.milzip.domain.store.service.StoreService;
import org.sku.milzip.global.common.BaseResponse;
import org.sku.milzip.global.common.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Store", description = "매장 조회 API")
@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

  private final StoreService storeService;

  @Operation(
      summary = "[ 전체 | 토큰 X | 매장 목록 페이지네이션 조회 ]",
      description =
          """
          **Purpose**
          - 군장병 할인업소 목록을 페이지네이션으로 조회합니다.

          **Query Parameters**
          - page: 페이지 번호 (기본값 0)
          - size: 페이지 크기 (기본값 20)
          - category: 카테고리 필터 (FOOD / CAFE / LEISURE / ACCOMMODATION / ETC, 미입력 시 전체)
          - sortBy: 정렬 기준 (discount=할인율순, distance=거리순, 미입력 시 조회수순)
          - lat: 위도 (sortBy=distance 시 필수)
          - lng: 경도 (sortBy=distance 시 필수)

          **Returns**
          - content: 매장 목록 (id, name, category, address, phone, 위도/경도, 혜택 여부, 최대 할인율, 거리(km))
          - page / size / totalElements / totalPages / last

          **Note**
          - sortBy=distance 사용 시 lat, lng 미입력이면 거리 계산이 불가하여 조회수순으로 대체됩니다
          - 위도/경도 미등록 매장(약 600건)은 거리순 정렬 결과에서 제외됩니다
          """)
  @GetMapping
  public BaseResponse<PageResponse<StoreListItemResponse>> getStores(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) StoreCategory category,
      @RequestParam(required = false) String sortBy,
      @RequestParam(required = false) Double lat,
      @RequestParam(required = false) Double lng) {
    return BaseResponse.success(storeService.getStores(category, page, size, sortBy, lat, lng));
  }

  @Operation(
      summary = "[ 전체 | 토큰 X | 매장 단건 조회 ]",
      description =
          """
          **Purpose**
          - 매장 ID로 상세 정보를 조회합니다.
          - 조회 시 viewCount가 1 증가합니다.

          **Returns**
          - 매장 기본 정보 (id, name, category, address, phone, 위도/경도, 운영시간, 휴일)
          - 혜택 목록 (description, discountRate, conditionText)

          **Error**
          - STO4041: 존재하지 않는 매장
          """)
  @GetMapping("/{id}")
  public BaseResponse<StoreDetailResponse> getStore(@PathVariable Long id) {
    return BaseResponse.success(storeService.getStore(id));
  }
}
