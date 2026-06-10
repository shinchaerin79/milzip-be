package org.sku.milzip.domain.benefit.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.sku.milzip.domain.benefit.dto.request.AmusementParkBenefitRequest;
import org.sku.milzip.domain.benefit.dto.request.MovieBenefitRequest;
import org.sku.milzip.domain.benefit.dto.request.SelfDevelopmentBenefitRequest;
import org.sku.milzip.domain.benefit.dto.response.AmusementParkBenefitResponse;
import org.sku.milzip.domain.benefit.dto.response.BenefitResponse;
import org.sku.milzip.domain.benefit.dto.response.BoxOfficeItemResponse;
import org.sku.milzip.domain.benefit.dto.response.MovieBenefitResponse;
import org.sku.milzip.domain.benefit.dto.response.SelfDevelopmentBenefitResponse;
import org.sku.milzip.domain.benefit.dto.response.TmoResponse;
import org.sku.milzip.domain.benefit.service.BenefitService;
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

@Tag(name = "Benefit", description = "군인 전용 혜택 API")
@RestController
@RequestMapping("/benefits")
@RequiredArgsConstructor
public class BenefitController {

  private final BenefitService benefitService;

  @Operation(
      summary = "[ 전체 | 토큰 X | TMO 위치 목록 조회 (거리순) ]",
      description =
          """
          **Purpose**
          - 전국 TMO(Terminal Maintenance Office) 목록을 조회합니다.
          - lat/lng 입력 시 현재 위치에서 가까운 순으로 정렬합니다.
          - 출장형 TMO는 isMobile: true 로 구분됩니다.
          """)
  @GetMapping("/tmo")
  public BaseResponse<List<TmoResponse>> getTmos(
      @Parameter(description = "위도", example = "37.9162") @RequestParam(required = false)
          Double lat,
      @Parameter(description = "경도", example = "127.1948") @RequestParam(required = false)
          Double lng) {
    return BaseResponse.success(benefitService.getTmos(lat, lng));
  }

  @Operation(
      summary = "[ 전체 | 토큰 X | 군인 영화 혜택 목록 조회 ]",
      description =
          """
          **Purpose**
          - CGV, 롯데시네마, 메가박스 등 영화관별 군인 할인 혜택 목록을 반환합니다.
          """)
  @GetMapping("/movies")
  public BaseResponse<List<MovieBenefitResponse>> getMovieBenefits() {
    return BaseResponse.success(benefitService.getMovieBenefits());
  }

  @Operation(
      summary = "[ 전체 | 토큰 X | 주간 박스오피스 조회 ]",
      description =
          """
          **Purpose**
          - 영화진흥위원회(KOBIS) 주간 박스오피스 상위 10편을 실시간으로 조회합니다.
          - 순위, 영화명, 장르, 상영시간, 누적 관객수를 반환합니다.
          """)
  @GetMapping("/movies/boxoffice")
  public BaseResponse<List<BoxOfficeItemResponse>> getWeeklyBoxOffice() {
    return BaseResponse.success(benefitService.getWeeklyBoxOffice());
  }

  @Operation(
      summary = "[ 전체 | 토큰 X | 군인 놀이공원 혜택 목록 조회 ]",
      description =
          """
          **Purpose**
          - 에버랜드, 롯데월드 등 놀이공원 군인 할인 혜택 목록을 반환합니다.
          - 위치, 유효기간, 원래 가격, 할인 가격, 증명 방법을 포함합니다.
          """)
  @GetMapping("/amusement-parks")
  public BaseResponse<List<AmusementParkBenefitResponse>> getAmusementParkBenefits() {
    return BaseResponse.success(benefitService.getAmusementParkBenefits());
  }

  @Operation(
      summary = "[ 전체 | 토큰 X | 자기계발 혜택 목록 조회 ]",
      description =
          """
          **Purpose**
          - 군인 대상 자기계발 지원 혜택 목록을 반환합니다.
          - 학업, 자격증, 취업, 지원금 카테고리별 정보를 포함합니다.
          - 온통청년 API 데이터 기반 (ETL로 주기적 업데이트).
          """)
  @GetMapping("/self-developments")
  public BaseResponse<PageResponse<SelfDevelopmentBenefitResponse>> getSelfDevelopmentBenefits(
      @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "페이지 크기", example = "10") @RequestParam(defaultValue = "10")
          int size,
      @Parameter(description = "카테고리 필터 (금융, 일자리, 주거, 교육, 문화, 참여·권리, 건강)", example = "교육")
          @RequestParam(required = false)
          String category) {
    return BaseResponse.success(benefitService.getSelfDevelopmentBenefits(page, size, category));
  }

  @Operation(summary = "[ 전체 | 토큰 X | 혜택 상세 조회 ]")
  @GetMapping("/{benefitId}")
  public BaseResponse<BenefitResponse> getBenefit(
      @Parameter(description = "혜택 ID") @PathVariable Long benefitId) {
    return BaseResponse.success(benefitService.getBenefit(benefitId));
  }

  // ── 영화 혜택 관리 ──────────────────────────────────────────

  @Operation(
      summary = "[ 관리자 | 토큰 O | 영화 혜택 등록 ]",
      security = @SecurityRequirement(name = "bearerAuth"))
  @PostMapping("/movies")
  @PreAuthorize("hasRole('ADMIN')")
  public BaseResponse<MovieBenefitResponse> createMovieBenefit(
      @Valid @RequestBody MovieBenefitRequest request) {
    return BaseResponse.success(benefitService.createMovieBenefit(request));
  }

  @Operation(
      summary = "[ 관리자 | 토큰 O | 영화 혜택 수정 ]",
      security = @SecurityRequirement(name = "bearerAuth"))
  @PutMapping("/movies/{benefitId}")
  @PreAuthorize("hasRole('ADMIN')")
  public BaseResponse<MovieBenefitResponse> updateMovieBenefit(
      @Parameter(description = "혜택 ID") @PathVariable Long benefitId,
      @Valid @RequestBody MovieBenefitRequest request) {
    return BaseResponse.success(benefitService.updateMovieBenefit(benefitId, request));
  }

  // ── 놀이공원 혜택 관리 ──────────────────────────────────────

  @Operation(
      summary = "[ 관리자 | 토큰 O | 놀이공원 혜택 등록 ]",
      security = @SecurityRequirement(name = "bearerAuth"))
  @PostMapping("/amusement-parks")
  @PreAuthorize("hasRole('ADMIN')")
  public BaseResponse<AmusementParkBenefitResponse> createAmusementPark(
      @Valid @RequestBody AmusementParkBenefitRequest request) {
    return BaseResponse.success(benefitService.createAmusementPark(request));
  }

  @Operation(
      summary = "[ 관리자 | 토큰 O | 놀이공원 혜택 수정 ]",
      security = @SecurityRequirement(name = "bearerAuth"))
  @PutMapping("/amusement-parks/{benefitId}")
  @PreAuthorize("hasRole('ADMIN')")
  public BaseResponse<AmusementParkBenefitResponse> updateAmusementPark(
      @Parameter(description = "혜택 ID") @PathVariable Long benefitId,
      @Valid @RequestBody AmusementParkBenefitRequest request) {
    return BaseResponse.success(benefitService.updateAmusementPark(benefitId, request));
  }

  // ── 자기계발 혜택 관리 ──────────────────────────────────────

  @Operation(
      summary = "[ 관리자 | 토큰 O | 자기계발 혜택 수정 ]",
      security = @SecurityRequirement(name = "bearerAuth"))
  @PutMapping("/self-developments/{benefitId}")
  @PreAuthorize("hasRole('ADMIN')")
  public BaseResponse<SelfDevelopmentBenefitResponse> updateSelfDevelopment(
      @Parameter(description = "혜택 ID") @PathVariable Long benefitId,
      @Valid @RequestBody SelfDevelopmentBenefitRequest request) {
    return BaseResponse.success(benefitService.updateSelfDevelopment(benefitId, request));
  }

  // ── 공통 삭제 ────────────────────────────────────────────────

  @Operation(
      summary = "[ 관리자 | 토큰 O | 혜택 삭제 (공통) ]",
      security = @SecurityRequirement(name = "bearerAuth"))
  @DeleteMapping("/{benefitId}")
  @PreAuthorize("hasRole('ADMIN')")
  public BaseResponse<Void> deleteBenefit(
      @Parameter(description = "혜택 ID") @PathVariable Long benefitId) {
    benefitService.deleteBenefit(benefitId);
    return BaseResponse.success(null);
  }
}
