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
      return tmoRepository.findAllWithCoordinates().stream()
          .map(
              tmo -> {
                double distance =
                    GeoUtils.calculateDistanceKm(lat, lng, tmo.getLatitude(), tmo.getLongitude());
                return TmoResponse.from(tmo, distance);
              })
          .sorted(Comparator.comparingDouble(TmoResponse::getDistanceKm))
          .toList();
    }
    return tmoRepository.findAll().stream().map(TmoResponse::from).toList();
  }

  /** 영화 혜택 목록 */
  @Transactional(readOnly = true)
  public List<MovieBenefitResponse> getMovieBenefits() {
    return benefitRepository.findByBenefitTypeOrderByIdAsc(BenefitType.MOVIE_BENEFIT).stream()
        .map(benefitMapper::toMovieResponse)
        .toList();
  }

  /** 주간 박스오피스 (ETL 적재 데이터 - Redis → DB 순) */
  public List<BoxOfficeItemResponse> getWeeklyBoxOffice() {
    return boxOfficeService.getWeeklyBoxOffice();
  }

  /** 놀이공원 혜택 목록 */
  @Transactional(readOnly = true)
  public List<AmusementParkBenefitResponse> getAmusementParkBenefits() {
    return benefitRepository.findByBenefitTypeOrderByIdAsc(BenefitType.AMUSEMENT_PARK).stream()
        .map(benefitMapper::toAmusementParkResponse)
        .toList();
  }

  /** 자기계발 혜택 목록 (페이지네이션) */
  @Transactional(readOnly = true)
  public PageResponse<SelfDevelopmentBenefitResponse> getSelfDevelopmentBenefits(
      int page, int size, String category) {
    PageRequest pageRequest = PageRequest.of(page, size);
    String normalizedCategory = normalizeCategoryFilter(category);

    return PageResponse.from(
        (normalizedCategory == null
                ? benefitRepository.findByBenefitTypeOrderByIdAsc(
                    BenefitType.SELF_DEVELOPMENT, pageRequest)
                : benefitRepository.findByBenefitTypeAndCategoryOrderByIdAsc(
                    BenefitType.SELF_DEVELOPMENT, normalizedCategory, pageRequest))
            .map(this::toSelfDevelopmentResponse));
  }

  /** 혜택 단건 조회 (공통 응답) */
  @Transactional(readOnly = true)
  public BenefitResponse getBenefit(Long benefitId) {
    return benefitRepository
        .findById(benefitId)
        .map(BenefitResponse::from)
        .orElseThrow(() -> new CustomException(BenefitErrorCode.BENEFIT_NOT_FOUND));
  }

  /** 영화 혜택 등록/수정 */
  @Transactional
  public MovieBenefitResponse createMovieBenefit(MovieBenefitRequest request) {
    return benefitMapper.toMovieResponse(
        benefitRepository.save(Benefit.createMovieBenefit(request)));
  }

  @Transactional
  public MovieBenefitResponse updateMovieBenefit(Long benefitId, MovieBenefitRequest request) {
    Benefit benefit = getBenefitEntityWithType(benefitId, BenefitType.MOVIE_BENEFIT);
    benefit.updateMovieBenefit(request);
    return benefitMapper.toMovieResponse(benefit);
  }

  /** 놀이공원 혜택 등록/수정 */
  @Transactional
  public AmusementParkBenefitResponse createAmusementPark(AmusementParkBenefitRequest request) {
    return benefitMapper.toAmusementParkResponse(
        benefitRepository.save(Benefit.createAmusementPark(request)));
  }

  @Transactional
  public AmusementParkBenefitResponse updateAmusementPark(
      Long benefitId, AmusementParkBenefitRequest request) {
    Benefit benefit = getBenefitEntityWithType(benefitId, BenefitType.AMUSEMENT_PARK);
    benefit.updateAmusementPark(request);
    return benefitMapper.toAmusementParkResponse(benefit);
  }

  /** 자기계발 혜택 수정 (등록은 ETL에서 처리) */
  @Transactional
  public SelfDevelopmentBenefitResponse updateSelfDevelopment(
      Long benefitId, SelfDevelopmentBenefitRequest request) {
    Benefit benefit = getBenefitEntityWithType(benefitId, BenefitType.SELF_DEVELOPMENT);
    benefit.updateSelfDevelopment(request);
    return toSelfDevelopmentResponse(benefit);
  }

  /** 혜택 삭제 (관리자, 공통) */
  @Transactional
  public void deleteBenefit(Long benefitId) {
    benefitRepository.delete(getBenefitEntity(benefitId));
  }

  private Benefit getBenefitEntity(Long benefitId) {
    return benefitRepository
        .findById(benefitId)
        .orElseThrow(() -> new CustomException(BenefitErrorCode.BENEFIT_NOT_FOUND));
  }

  private Benefit getBenefitEntityWithType(Long benefitId, BenefitType expectedType) {
    Benefit benefit = getBenefitEntity(benefitId);
    if (benefit.getBenefitType() != expectedType) {
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
