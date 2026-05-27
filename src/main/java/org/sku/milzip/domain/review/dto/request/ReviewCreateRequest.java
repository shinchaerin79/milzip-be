package org.sku.milzip.domain.review.dto.request;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.sku.milzip.domain.review.entity.BenefitStatus;
import org.sku.milzip.domain.review.entity.GoodPoint;
import org.sku.milzip.domain.review.entity.VisitPurpose;
import org.sku.milzip.domain.review.entity.VisitType;
import org.sku.milzip.domain.review.entity.VisitWith;
import org.sku.milzip.domain.review.entity.WaitTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "리뷰 작성/수정 요청")
public class ReviewCreateRequest {

  @Schema(description = "별점 (1~5)", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "별점을 입력해주세요.") @Min(value = 1, message = "별점은 최소 1점입니다.")
  @Max(value = 5, message = "별점은 최대 5점입니다.")
  private Integer rating;

  @Schema(
      description = "군인 할인 혜택 여부 (SOLDIER 인증 유저만 입력)",
      allowableValues = {"RECEIVED", "NOT_RECEIVED", "PARTIAL"},
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private BenefitStatus benefitStatus;

  @Schema(
      description = "이용 방식",
      allowableValues = {"WALK_IN", "RESERVED", "TAKEOUT_DELIVERY"},
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "이용 방식을 선택해주세요.") private VisitType visitType;

  @Schema(
      description = "대기 시간",
      allowableValues = {
        "IMMEDIATE",
        "WITHIN_10_MIN",
        "WITHIN_30_MIN",
        "WITHIN_1_HOUR",
        "OVER_1_HOUR"
      },
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "대기 시간을 선택해주세요.") private WaitTime waitTime;

  @Schema(
      description = "방문 목적",
      allowableValues = {"DATE", "OUTING", "OVERNIGHT_PASS", "VACATION", "GATHERING"},
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "방문 목적을 선택해주세요.") private VisitPurpose visitPurpose;

  @Schema(
      description = "동행자",
      allowableValues = {"COUPLE", "FAMILY", "FRIEND", "ALONE"},
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "동행자를 선택해주세요.") private VisitWith visitWith;

  @Schema(
      description = "좋았던 점 (복수 선택 가능)",
      example = "[\"TASTY\", \"GOOD_VALUE\"]",
      allowableValues = {
        "TASTY",
        "LARGE_PORTION",
        "GOOD_VALUE",
        "GOOD_FOR_SOLO",
        "GOOD_FOR_GROUPS",
        "QUIET"
      })
  private List<GoodPoint> goodPoints;

  @Schema(description = "텍스트 리뷰 (선택)", example = "군장병 할인이 잘 적용되고 음식도 맛있었어요!")
  @Size(max = 500, message = "리뷰는 500자 이내로 작성해주세요.")
  private String content;
}
