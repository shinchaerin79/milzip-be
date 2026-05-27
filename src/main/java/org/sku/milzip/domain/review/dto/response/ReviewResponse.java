package org.sku.milzip.domain.review.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import org.sku.milzip.domain.review.entity.BenefitStatus;
import org.sku.milzip.domain.review.entity.GoodPoint;
import org.sku.milzip.domain.review.entity.Review;
import org.sku.milzip.domain.review.entity.ReviewStatus;
import org.sku.milzip.domain.review.entity.VisitPurpose;
import org.sku.milzip.domain.review.entity.VisitType;
import org.sku.milzip.domain.review.entity.VisitWith;
import org.sku.milzip.domain.review.entity.WaitTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "리뷰 응답")
public class ReviewResponse {

  @Schema(description = "리뷰 ID", example = "1")
  private Long id;

  @Schema(description = "매장 ID", example = "10")
  private Long storeId;

  @Schema(description = "작성자 ID", example = "3")
  private Long userId;

  @Schema(description = "작성자 닉네임", example = "포천병사")
  private String nickname;

  @Schema(description = "별점 (1~5)", example = "5")
  private int rating;

  @Schema(description = "군인 할인 혜택 여부")
  private BenefitStatus benefitStatus;

  @Schema(description = "이용 방식")
  private VisitType visitType;

  @Schema(description = "대기 시간")
  private WaitTime waitTime;

  @Schema(description = "방문 목적")
  private VisitPurpose visitPurpose;

  @Schema(description = "동행자")
  private VisitWith visitWith;

  @Schema(description = "좋았던 점")
  private List<GoodPoint> goodPoints;

  @Schema(description = "텍스트 리뷰", example = "군장병 할인이 잘 적용되고 음식도 맛있었어요!")
  private String content;

  @Schema(description = "리뷰 상태 (VISIBLE / HIDDEN)")
  private ReviewStatus status;

  @Schema(description = "작성일시")
  private LocalDateTime createdAt;

  @Schema(description = "수정일시")
  private LocalDateTime modifiedAt;

  public static ReviewResponse from(Review review) {
    return ReviewResponse.builder()
        .id(review.getId())
        .storeId(review.getStore().getId())
        .userId(review.getUser().getId())
        .nickname(review.getUser().getNickname())
        .rating(review.getRating())
        .benefitStatus(review.getBenefitStatus())
        .visitType(review.getVisitType())
        .waitTime(review.getWaitTime())
        .visitPurpose(review.getVisitPurpose())
        .visitWith(review.getVisitWith())
        .goodPoints(review.getGoodPoints())
        .content(review.getContent())
        .status(review.getStatus())
        .createdAt(review.getCreatedAt())
        .modifiedAt(review.getModifiedAt())
        .build();
  }
}
