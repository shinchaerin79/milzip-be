package org.sku.milzip.domain.benefit.service;

import java.util.Comparator;
import java.util.List;

import org.sku.milzip.domain.benefit.dto.request.AmusementParkBenefitRequest;
import org.sku.milzip.domain.benefit.dto.request.MovieBenefitRequest;
import org.sku.milzip.domain.benefit.dto.request.SelfDevelopmentBenefitRequest;
import org.sku.milzip.domain.benefit.dto.response.AmusementParkBenefitResponse;
import org.sku.milzip.domain.benefit.dto.response.BenefitResponse;
import org.sku.milzip.domain.benefit.dto.response.BoxOfficeItemResponse;
import org.sku.milzip.domain.benefit.dto.response.MovieBenefitResponse;
import org.sku.milzip.domain.benefit.dto.response.SelfDevelopmentBenefitResponse;
import org.sku.milzip.domain.benefit.dto.response.TmoResponse;
import org.sku.milzip.domain.benefit.entity.Benefit;
import org.sku.milzip.domain.benefit.entity.BenefitType;
import org.sku.milzip.domain.benefit.exception.BenefitErrorCode;
import org.sku.milzip.domain.benefit.mapper.BenefitMapper;
import org.sku.milzip.domain.benefit.repository.BenefitRepository;
import org.sku.milzip.domain.benefit.repository.TmoRepository;
import org.sku.milzip.global.common.PageResponse;
import org.sku.milzip.global.exception.CustomException;
import org.sku.milzip.global.util.GeoUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BenefitService {

  private final BenefitRepository benefitRepository;
  private final TmoRepository tmoRepository;
  private final BoxOfficeService boxOfficeService;
  private final BenefitMapper benefitMapper;

  /** TMO 목록 조회 (좌표 제공 시 거리순, 미제공 시 전체) */
  @Transactional(readOnly = true)
  public List<TmoResponse> getTmos(Double lat, Double lng) {
    if (lat != null && lng != null) {
      log.debug("[BenefitService] TMO 목록 조회 (거리순) - lat: {}, lng: {}", lat, lng);
      List<TmoResponse> result =
          tmoRepository.findAllWithCoordinates().stream()
              .map(
                  tmo -> {
                    double distance =
                        GeoUtils.calculateDistanceKm(
                            lat, lng, tmo.getLatitude(), tmo.getLongitude());
                    return TmoResponse.from(tmo, distance);
                  })
              .sorted(Comparator.comparingDouble(TmoResponse::getDistanceKm))
              .toList();
      log.debug("[BenefitService] TMO 목록 조회 완료 - 총 {}개소", result.size());
      return result;
    }
    log.debug("[BenefitService] TMO 목록 조회 (전체)");
    List<TmoResponse> result = tmoRepository.findAll().stream().map(TmoResponse::from).toList();
    log.debug("[BenefitService] TMO 목록 조회 완료 - 총 {}개소", result.size());
    return result;
  }

  /** 영화 혜택 목록 */
  @Transactional(readOnly = true)
  public List<MovieBenefitResponse> getMovieBenefits() {
    log.debug("[BenefitService] 영화 혜택 목록 조회");
    List<MovieBenefitResponse> result =
        benefitRepository.findByBenefitTypeOrderByIdAsc(BenefitType.MOVIE_BENEFIT).stream()
            .map(benefitMapper::toMovieResponse)
            .toList();
    log.debug("[BenefitService] 영화 혜택 목록 조회 완료 - {}건", result.size());
    return result;
  }

  /** 주간 박스오피스 (ETL 적재 데이터 - Redis → DB 순) */
  public List<BoxOfficeItemResponse> getWeeklyBoxOffice() {
    log.debug("[BenefitService] 주간 박스오피스 조회 요청");
    return boxOfficeService.getWeeklyBoxOffice();
  }

  /** 놀이공원 혜택 목록 */
  @Transactional(readOnly = true)
  public List<AmusementParkBenefitResponse> getAmusementParkBenefits() {
    log.debug("[BenefitService] 놀이공원 혜택 목록 조회");
    List<AmusementParkBenefitResponse> result =
        benefitRepository.findByBenefitTypeOrderByIdAsc(BenefitType.AMUSEMENT_PARK).stream()
            .map(benefitMapper::toAmusementParkResponse)
            .toList();
    log.debug("[BenefitService] 놀이공원 혜택 목록 조회 완료 - {}건", result.size());
    return result;
  }

  /** 자기계발 혜택 목록 (페이지네이션) */
  @Transactional(readOnly = true)
  public PageResponse<SelfDevelopmentBenefitResponse> getSelfDevelopmentBenefits(
      int page, int size, String category) {
    log.debug(
        "[BenefitService] 자기계발 혜택 목록 조회 - page: {}, size: {}, category: {}", page, size, category);
    PageRequest pageRequest = PageRequest.of(page, size);
    String normalizedCategory = normalizeCategoryFilter(category);

    PageResponse<SelfDevelopmentBenefitResponse> result =
        PageResponse.from(
            (normalizedCategory == null
                    ? benefitRepository.findByBenefitTypeOrderByIdAsc(
                        BenefitType.SELF_DEVELOPMENT, pageRequest)
                    : benefitRepository.findByBenefitTypeAndCategoryOrderByIdAsc(
                        BenefitType.SELF_DEVELOPMENT, normalizedCategory, pageRequest))
                .map(this::toSelfDevelopmentResponse));
    log.debug("[BenefitService] 자기계발 혜택 목록 조회 완료 - 총 {}건", result.getTotalElements());
    return result;
  }

  /** 혜택 단건 조회 (공통 응답) */
  @Transactional(readOnly = true)
  public BenefitResponse getBenefit(Long benefitId) {
    log.debug("[BenefitService] 혜택 단건 조회 - benefitId: {}", benefitId);
    return benefitRepository
        .findById(benefitId)
        .map(BenefitResponse::from)
        .orElseThrow(
            () -> {
              log.warn("[BenefitService] 혜택 없음 - benefitId: {}", benefitId);
              return new CustomException(BenefitErrorCode.BENEFIT_NOT_FOUND);
            });
  }

  /** 영화 혜택 등록/수정 */
  @Transactional
  public MovieBenefitResponse createMovieBenefit(MovieBenefitRequest request) {
    log.info("[BenefitService] 영화 혜택 등록 - cinemaChain: {}", request.getCinemaChain());
    MovieBenefitResponse response =
        benefitMapper.toMovieResponse(benefitRepository.save(Benefit.createMovieBenefit(request)));
    log.info("[BenefitService] 영화 혜택 등록 완료 - benefitId: {}", response.getId());
    return response;
  }

  @Transactional
  public MovieBenefitResponse updateMovieBenefit(Long benefitId, MovieBenefitRequest request) {
    log.info("[BenefitService] 영화 혜택 수정 - benefitId: {}", benefitId);
    Benefit benefit = getBenefitEntityWithType(benefitId, BenefitType.MOVIE_BENEFIT);
    benefit.updateMovieBenefit(request);
    log.info("[BenefitService] 영화 혜택 수정 완료 - benefitId: {}", benefitId);
    return benefitMapper.toMovieResponse(benefit);
  }

  /** 놀이공원 혜택 등록/수정 */
  @Transactional
  public AmusementParkBenefitResponse createAmusementPark(AmusementParkBenefitRequest request) {
    log.info("[BenefitService] 놀이공원 혜택 등록 - title: {}", request.getTitle());
    AmusementParkBenefitResponse response =
        benefitMapper.toAmusementParkResponse(
            benefitRepository.save(Benefit.createAmusementPark(request)));
    log.info("[BenefitService] 놀이공원 혜택 등록 완료 - benefitId: {}", response.getId());
    return response;
  }

  @Transactional
  public AmusementParkBenefitResponse updateAmusementPark(
      Long benefitId, AmusementParkBenefitRequest request) {
    log.info("[BenefitService] 놀이공원 혜택 수정 - benefitId: {}", benefitId);
    Benefit benefit = getBenefitEntityWithType(benefitId, BenefitType.AMUSEMENT_PARK);
    benefit.updateAmusementPark(request);
    log.info("[BenefitService] 놀이공원 혜택 수정 완료 - benefitId: {}", benefitId);
    return benefitMapper.toAmusementParkResponse(benefit);
  }

  /** 자기계발 혜택 수정 (등록은 ETL에서 처리) */
  @Transactional
  public SelfDevelopmentBenefitResponse updateSelfDevelopment(
      Long benefitId, SelfDevelopmentBenefitRequest request) {
    log.info("[BenefitService] 자기계발 혜택 수정 - benefitId: {}", benefitId);
    Benefit benefit = getBenefitEntityWithType(benefitId, BenefitType.SELF_DEVELOPMENT);
    benefit.updateSelfDevelopment(request);
    log.info("[BenefitService] 자기계발 혜택 수정 완료 - benefitId: {}", benefitId);
    return toSelfDevelopmentResponse(benefit);
  }

  /** 혜택 삭제 (관리자, 공통) */
  @Transactional
  public void deleteBenefit(Long benefitId) {
    log.info("[BenefitService] 혜택 삭제 - benefitId: {}", benefitId);
    benefitRepository.delete(getBenefitEntity(benefitId));
    log.info("[BenefitService] 혜택 삭제 완료 - benefitId: {}", benefitId);
  }

  private Benefit getBenefitEntity(Long benefitId) {
    return benefitRepository
        .findById(benefitId)
        .orElseThrow(
            () -> {
              log.warn("[BenefitService] 혜택 없음 - benefitId: {}", benefitId);
              return new CustomException(BenefitErrorCode.BENEFIT_NOT_FOUND);
            });
  }

  private Benefit getBenefitEntityWithType(Long benefitId, BenefitType expectedType) {
    Benefit benefit = getBenefitEntity(benefitId);
    if (benefit.getBenefitType() != expectedType) {
      log.warn(
          "[BenefitService] 혜택 타입 불일치 - benefitId: {}, 기대: {}, 실제: {}",
          benefitId,
          expectedType,
          benefit.getBenefitType());
      throw new CustomException(BenefitErrorCode.BENEFIT_TYPE_MISMATCH);
    }
    return benefit;
  }

  private SelfDevelopmentBenefitResponse toSelfDevelopmentResponse(Benefit benefit) {
    SelfDevelopmentBenefitResponse response = benefitMapper.toSelfDevelopmentResponse(benefit);
    return SelfDevelopmentBenefitResponse.builder()
        .id(response.getId())
        .title(response.getTitle())
        .description(response.getDescription())
        .imageUrl(response.getImageUrl())
        .category(normalizeCategoryDisplay(response.getCategory()))
        .applyUrl(response.getApplyUrl())
        .supportType(response.getSupportType())
        .build();
  }

  private String normalizeCategoryFilter(String category) {
    if (category == null || category.isBlank()) {
      return null;
    }

    return switch (category.trim()) {
      case "금융", "복지·금융" -> "복지·금융";
      case "문화", "복지·문화" -> "복지·문화";
      default -> category.trim();
    };
  }

  private String normalizeCategoryDisplay(String category) {
    if (category == null) {
      return null;
    }

    return switch (category) {
      case "복지·금융" -> "금융";
      case "복지·문화" -> "문화";
      default -> category;
    };
  }
}
