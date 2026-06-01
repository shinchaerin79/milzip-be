package org.sku.milzip.domain.benefit.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.sku.milzip.domain.benefit.entity.BenefitType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "혜택 등록/수정 요청")
public class BenefitCreateRequest {

  @NotNull @Schema(description = "혜택 유형 (MOVIE_BENEFIT / AMUSEMENT_PARK / SELF_DEVELOPMENT)")
  private BenefitType benefitType;

  @NotBlank
  @Schema(description = "혜택 제목")
  private String title;

  @Schema(description = "혜택 설명")
  private String description;

  @Schema(description = "이미지 URL")
  private String imageUrl;

  // 영화 혜택
  @Schema(description = "영화관 체인 (MOVIE_BENEFIT 전용)", example = "CGV")
  private String cinemaChain;

  // 놀이공원 혜택
  @Schema(description = "지역 (AMUSEMENT_PARK 전용)", example = "수도권")
  private String region;

  @Schema(description = "위치 (AMUSEMENT_PARK 전용)", example = "경기 용인")
  private String location;

  @Schema(description = "혜택 시작일")
  private LocalDate validFrom;

  @Schema(description = "혜택 종료일")
  private LocalDate validUntil;

  @Schema(description = "원래 가격 (원)")
  private Integer originalPrice;

  @Schema(description = "할인 가격 (원, 0이면 무료)")
  private Integer discountedPrice;

  @Schema(description = "할인 설명", example = "무료")
  private String discountDescription;

  @Schema(description = "증명 방법", example = "휴가증, 병역증명서")
  private String verificationMethod;

  // 자기계발 혜택
  @Schema(description = "카테고리 (SELF_DEVELOPMENT 전용)", example = "학업")
  private String category;

  @Schema(description = "신청 URL")
  private String applyUrl;

  @Schema(description = "지원 유형", example = "무료")
  private String supportType;
}
