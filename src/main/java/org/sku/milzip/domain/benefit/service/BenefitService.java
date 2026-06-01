package org.sku.milzip.domain.benefit.service;

import java.util.Comparator;
import java.util.List;

import org.sku.milzip.domain.benefit.dto.request.AmusementParkBenefitRequest;
import org.sku.milzip.domain.benefit.dto.request.MovieBenefitRequest;
import org.sku.milzip.domain.benefit.dto.request.SelfDevelopmentBenefitRequest;
import org.sku.milzip.domain.benefit.dto.response.BenefitResponse;
import org.sku.milzip.domain.benefit.dto.response.BoxOfficeItemResponse;
import org.sku.milzip.domain.benefit.dto.response.TmoResponse;
import org.sku.milzip.domain.benefit.entity.Benefit;
import org.sku.milzip.domain.benefit.entity.BenefitType;
import org.sku.milzip.domain.benefit.exception.BenefitErrorCode;
import org.sku.milzip.domain.benefit.repository.BenefitRepository;
import org.sku.milzip.domain.benefit.repository.TmoRepository;
import org.sku.milzip.global.exception.CustomException;
import org.sku.milzip.global.util.GeoUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BenefitService {

  private final BenefitRepository benefitRepository;
  private final TmoRepository tmoRepository;
  private final BoxOfficeService boxOfficeService;

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

  /** 영화 혜택 목록 (영화관 할인 정보) */
  @Transactional(readOnly = true)
  public List<BenefitResponse> getMovieBenefits() {
    return benefitRepository.findByBenefitTypeOrderByIdAsc(BenefitType.MOVIE_BENEFIT).stream()
        .map(BenefitResponse::from)
        .toList();
  }

  /** 주간 박스오피스 (ETL 적재 데이터 - Redis → DB 순) */
  public List<BoxOfficeItemResponse> getWeeklyBoxOffice() {
    return boxOfficeService.getWeeklyBoxOffice();
  }

  /** 놀이공원 혜택 목록 */
  @Transactional(readOnly = true)
  public List<BenefitResponse> getAmusementParkBenefits() {
    return benefitRepository.findByBenefitTypeOrderByIdAsc(BenefitType.AMUSEMENT_PARK).stream()
        .map(BenefitResponse::from)
        .toList();
  }

  /** 자기계발 혜택 목록 (온통청년 API 데이터 기반, ETL로 적재) */
  @Transactional(readOnly = true)
  public List<BenefitResponse> getSelfDevelopmentBenefits() {
    return benefitRepository.findByBenefitTypeOrderByIdAsc(BenefitType.SELF_DEVELOPMENT).stream()
        .map(BenefitResponse::from)
        .toList();
  }

  /** 혜택 단건 조회 */
  @Transactional(readOnly = true)
  public BenefitResponse getBenefit(Long benefitId) {
    return benefitRepository
        .findById(benefitId)
        .map(BenefitResponse::from)
        .orElseThrow(() -> new CustomException(BenefitErrorCode.BENEFIT_NOT_FOUND));
  }

  /** 영화 혜택 등록/수정 */
  @Transactional
  public BenefitResponse createMovieBenefit(MovieBenefitRequest request) {
    return BenefitResponse.from(benefitRepository.save(Benefit.createMovieBenefit(request)));
  }

  @Transactional
  public BenefitResponse updateMovieBenefit(Long benefitId, MovieBenefitRequest request) {
    Benefit benefit = getBenefitEntity(benefitId);
    benefit.updateMovieBenefit(request);
    return BenefitResponse.from(benefit);
  }

  /** 놀이공원 혜택 등록/수정 */
  @Transactional
  public BenefitResponse createAmusementPark(AmusementParkBenefitRequest request) {
    return BenefitResponse.from(benefitRepository.save(Benefit.createAmusementPark(request)));
  }

  @Transactional
  public BenefitResponse updateAmusementPark(Long benefitId, AmusementParkBenefitRequest request) {
    Benefit benefit = getBenefitEntity(benefitId);
    benefit.updateAmusementPark(request);
    return BenefitResponse.from(benefit);
  }

  /** 자기계발 혜택 등록/수정 */
  @Transactional
  public BenefitResponse createSelfDevelopment(SelfDevelopmentBenefitRequest request) {
    return BenefitResponse.from(benefitRepository.save(Benefit.createSelfDevelopment(request)));
  }

  @Transactional
  public BenefitResponse updateSelfDevelopment(
      Long benefitId, SelfDevelopmentBenefitRequest request) {
    Benefit benefit = getBenefitEntity(benefitId);
    benefit.updateSelfDevelopment(request);
    return BenefitResponse.from(benefit);
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
}
