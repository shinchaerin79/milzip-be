package org.sku.milzip.domain.store.controller;

import jakarta.validation.Valid;

import org.sku.milzip.domain.store.dto.request.StoreCreateRequest;
import org.sku.milzip.domain.store.dto.response.StoreDetailResponse;
import org.sku.milzip.domain.store.dto.response.StoreListItemResponse;
import org.sku.milzip.domain.store.entity.StoreCategory;
import org.sku.milzip.domain.store.service.StoreService;
import org.sku.milzip.global.common.BaseResponse;
import org.sku.milzip.global.common.PageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Store", description = "매장 조회 API")
@RestController
@RequestMapping("/stores")
@RequiredArgsConstructor
public class StoreController {

  private final StoreService storeService;

  @Operation(
      summary = "[ 전체 | 토큰 X | 매장 목록 페이지네이션 조회 ]",
      description =
          """
          **Purpose**
          - 군장병 할인업소 목록을 페이지네이션으로 조회합니다. 무한스크롤 구현 시 page를 증가시켜 호출하세요.

          **Query Parameters**
          - page: 페이지 번호 (기본값 0)
          - size: 페이지 크기 (기본값 20)
          - category: 카테고리 필터 (FOOD / ACCOMMODATION / PC_CAFE / SERVICE, 미입력 시 전체)
            · FOOD = 음식(구 FOOD+CAFE 통합)
            · ACCOMMODATION = 숙박
            · PC_CAFE = PC방 (이름에 'pc', 'pc방' 포함 매장)
            · SERVICE = 서비스 (구 여가+기타 중 PC방 제외)
          - sortBy: 정렬 기준 (미입력 시 조회수순) — lat/lng 입력 시 거리순 자동 전환
          - lat: 현재 위도 (입력 시 거리순 정렬로 자동 전환)
          - lng: 현재 경도 (입력 시 거리순 정렬로 자동 전환)
          - radius: 반경 필터 (km, lat/lng 필수). 지정 시 해당 반경 내 매장만 반환
          - keyword: 매장명 검색어 (부분 일치, 대소문자 무시)

          **Returns**
          - content: 매장 목록 (id, name, category, address, phone, 위도/경도, 혜택 여부, 최대 할인율, 거리(km))
          - page / size / totalElements / totalPages / last

          **Note**
          - lat/lng를 넘기면 sortBy 값에 관계없이 거리순으로 자동 전환됩니다
          - 좌표가 없는 매장은 거리순 결과에서 제외됩니다 (카카오 enrichment 미완료 매장)
          - radius는 lat/lng 없이 단독 사용 불가 (무시됩니다)
          """)
  @GetMapping
  public BaseResponse<PageResponse<StoreListItemResponse>> getStores(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) StoreCategory category,
      @RequestParam(required = false) String sortBy,
      @RequestParam(required = false) Double lat,
      @RequestParam(required = false) Double lng,
      @Parameter(description = "반경 필터 (km). lat/lng와 함께 사용") @RequestParam(required = false)
          Double radius,
      @Parameter(description = "매장명 검색어 (부분 일치)") @RequestParam(required = false) String keyword) {
    return BaseResponse.success(
        storeService.getStores(category, page, size, sortBy, lat, lng, radius, keyword));
  }

  @Operation(
      summary = "[ 전체 | 토큰 X | 조회수 기준 BEST 매장 단건 조회 ]",
      description =
          """
          **Purpose**
          - 전체 매장 중 조회수(viewCount)가 가장 높은 매장 1개를 반환합니다.

          **Error**
          - STO4041: 등록된 매장이 없는 경우
          """)
  @GetMapping("/best")
  public BaseResponse<StoreDetailResponse> getBestStore() {
    return BaseResponse.success(storeService.getBestStore());
  }

  @Operation(
      summary = "[ 관리자 | 토큰 O | 매장 등록 ]",
      security = @SecurityRequirement(name = "bearerAuth"),
      description =
          """
          **Purpose**
          - 관리자가 새로운 매장을 등록합니다.

          **Error**
          - AUTH4011: 토큰 미포함 또는 만료
          - G003: 권한 없음 (403)
          """)
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public BaseResponse<StoreDetailResponse> createStore(
      @Valid @RequestBody StoreCreateRequest request) {
    return BaseResponse.success(storeService.createStore(request));
  }

  @Operation(
      summary = "[ 관리자 | 토큰 O | 매장 수정 ]",
      security = @SecurityRequirement(name = "bearerAuth"),
      description =
          """
          **Purpose**
          - 관리자가 기존 매장 정보를 수정합니다.
          - benefits 목록은 전체 교체(replace) 방식으로 동작합니다.

          **Error**
          - STO4041: 존재하지 않는 매장
          - AUTH4011: 토큰 미포함 또는 만료
          - G003: 권한 없음 (403)
          """)
  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public BaseResponse<StoreDetailResponse> updateStore(
      @Parameter(description = "매장 ID") @PathVariable Long id,
      @Valid @RequestBody StoreCreateRequest request) {
    return BaseResponse.success(storeService.updateStore(id, request));
  }

  @Operation(
      summary = "[ 관리자 | 토큰 O | 매장 삭제 ]",
      security = @SecurityRequirement(name = "bearerAuth"),
      description =
          """
          **Purpose**
          - 관리자가 매장을 삭제합니다. 연관된 혜택 정보도 함께 삭제됩니다.

          **Error**
          - STO4041: 존재하지 않는 매장
          - AUTH4011: 토큰 미포함 또는 만료
          - G003: 권한 없음 (403)
          """)
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public BaseResponse<Void> deleteStore(@Parameter(description = "매장 ID") @PathVariable Long id) {
    storeService.deleteStore(id);
    return BaseResponse.success(null);
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
