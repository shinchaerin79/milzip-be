package org.sku.milzip.domain.review.controller;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.sku.milzip.domain.review.dto.request.ReviewCreateRequest;
import org.sku.milzip.domain.review.dto.request.ReviewStatusRequest;
import org.sku.milzip.domain.review.dto.response.ReceiptVerifyResponse;
import org.sku.milzip.domain.review.dto.response.ReviewResponse;
import org.sku.milzip.domain.review.entity.BenefitStatus;
import org.sku.milzip.domain.review.entity.GoodPoint;
import org.sku.milzip.domain.review.entity.VisitPurpose;
import org.sku.milzip.domain.review.entity.VisitType;
import org.sku.milzip.domain.review.entity.VisitWith;
import org.sku.milzip.domain.review.entity.WaitTime;
import org.sku.milzip.domain.review.service.ReceiptOcrService;
import org.sku.milzip.domain.review.service.ReviewService;
import org.sku.milzip.global.common.BaseResponse;
import org.sku.milzip.global.common.PageResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Review", description = "매장 리뷰 API")
@Validated
@RestController
@RequestMapping("/stores/{storeId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

  private final ReviewService reviewService;
  private final ReceiptOcrService receiptOcrService;

  @Operation(
      summary = "[ 사용자 | 토큰 O | 영수증 OCR 검증 ]",
      security = @SecurityRequirement(name = "bearerAuth"),
      description =
          """
          **Purpose**
          - 리뷰 작성 전, 영수증 이미지를 업로드하여 해당 매장과 일치하는지 검증합니다.

          **Flow**
          1. 리뷰 작성 버튼 클릭
          2. 영수증 촬영 → 이 API 호출
          3. verified: true 이면 리뷰 작성 폼으로 진행

          **Returns**
          - verified: 매장 일치 여부
          - recognizedStoreName: OCR 인식된 가게명
          - message: 검증 결과 메시지

          **Note**
          - 현재 Naver Clova OCR 미연동 상태 (TODO)
          """)
  @PostMapping(value = "/receipt-verify", consumes = "multipart/form-data")
  @PreAuthorize("isAuthenticated()")
  public BaseResponse<ReceiptVerifyResponse> verifyReceipt(
      @Parameter(description = "매장 ID") @PathVariable Long storeId,
      @Parameter(description = "영수증 이미지 파일") @RequestParam("receiptImage")
          MultipartFile receiptImage) {
    return BaseResponse.success(receiptOcrService.verifyReceipt(storeId, receiptImage));
  }

  @Operation(
      summary = "[ 전체 | 토큰 X | 매장 리뷰 단건 조회 ]",
      description =
          """
          **Purpose**
          - 특정 매장의 리뷰 상세 정보를 조회합니다.

          **Error**
          - STO4041: 존재하지 않는 매장
          - REV4041: 존재하지 않는 리뷰
          """)
  @GetMapping("/{reviewId}")
  public BaseResponse<ReviewResponse> getReview(
      @Parameter(description = "매장 ID") @PathVariable Long storeId,
      @Parameter(description = "리뷰 ID") @PathVariable Long reviewId) {
    return BaseResponse.success(reviewService.getReview(storeId, reviewId));
  }

  @Operation(
      summary = "[ 전체 | 토큰 X | 매장 리뷰 목록 조회 ]",
      description =
          """
          **Purpose**
          - 특정 매장의 리뷰 목록을 최신순으로 조회합니다.

          **Query Parameters**
          - page: 페이지 번호 (기본값 0)
          - size: 페이지 크기 (기본값 10)

          **Returns**
          - 숨김(HIDDEN) 처리된 리뷰는 제외하고 반환합니다.

          **Error**
          - STO4041: 존재하지 않는 매장
          """)
  @GetMapping
  public BaseResponse<PageResponse<ReviewResponse>> getReviews(
      @Parameter(description = "매장 ID") @PathVariable Long storeId,
      @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "페이지 크기", example = "10") @RequestParam(defaultValue = "10")
          int size) {
    return BaseResponse.success(reviewService.getReviews(storeId, page, size));
  }

  @Operation(
      summary = "[ 사용자 | 토큰 O | 매장 리뷰 작성 ]",
      security = @SecurityRequirement(name = "bearerAuth"),
      description =
          """
          **Purpose**
          - 특정 매장에 리뷰를 작성합니다.

          **Constraints**
          - 매장당 1인 1개의 리뷰만 작성 가능합니다.
          - 별점은 1~5점 사이로 입력해야 합니다.
          - 리뷰 내용은 500자 이내로 작성해야 합니다.

          **Parameters**
          - rating: 별점 (1~5)
          - benefitStatus: 군인 할인 혜택 여부 (RECEIVED / NOT_RECEIVED / PARTIAL) — SOLDIER 인증 유저 필수, MEMBER 생략 가능
          - visitType: 이용 방식 (WALK_IN / RESERVED / TAKEOUT_DELIVERY)
          - waitTime: 대기 시간 (IMMEDIATE / WITHIN_10_MIN / WITHIN_30_MIN / WITHIN_1_HOUR / OVER_1_HOUR)
          - visitPurpose: 방문 목적 (DATE / OUTING / OVERNIGHT_PASS / VACATION / GATHERING)
          - visitWith: 동행자 (COUPLE / FAMILY / FRIEND / ALONE)
          - goodPoints: 좋았던 점 (복수 선택, 선택 사항)
          - content: 텍스트 리뷰 (선택 사항, 500자 이내)

          **Parameters**
          - images: 리뷰 이미지 (선택, 최대 3개)

          **Error**
          - STO4041: 존재하지 않는 매장
          - REV4091: 이미 해당 매장에 리뷰를 작성한 경우
          - AUTH4011: 토큰 미포함 또는 만료
          """)
  @PostMapping(
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
  @PreAuthorize("isAuthenticated()")
  public BaseResponse<ReviewResponse> createReview(
      @Parameter(description = "매장 ID") @PathVariable Long storeId,
      @AuthenticationPrincipal String email,
      @Parameter(description = "별점 (1~5)") @RequestParam @NotNull @Min(1) @Max(5) Integer rating,
      @Parameter(
              description =
                  "군인 할인 혜택 여부 (SOLDIER 인증 유저 필수 / MEMBER 생략 가능)\n"
                      + "- RECEIVED: 혜택 받음\n"
                      + "- NOT_RECEIVED: 혜택 받지 못함\n"
                      + "- PARTIAL: 일부 받음")
          @RequestParam(required = false)
          BenefitStatus benefitStatus,
      @Parameter(
              description =
                  "이용 방식\n"
                      + "- WALK_IN: 예약 없이 이용\n"
                      + "- RESERVED: 예약 후 이용\n"
                      + "- TAKEOUT_DELIVERY: 포장·배달 이용")
          @RequestParam
          @NotNull VisitType visitType,
      @Parameter(
              description =
                  "대기 시간\n"
                      + "- IMMEDIATE: 바로 입장\n"
                      + "- WITHIN_10_MIN: 10분 이내\n"
                      + "- WITHIN_30_MIN: 30분 이내\n"
                      + "- WITHIN_1_HOUR: 1시간 이내\n"
                      + "- OVER_1_HOUR: 1시간 이상")
          @RequestParam
          @NotNull WaitTime waitTime,
      @Parameter(
              description =
                  "방문 목적\n"
                      + "- DATE: 데이트\n"
                      + "- OUTING: 외출\n"
                      + "- OVERNIGHT_PASS: 외박\n"
                      + "- VACATION: 휴가\n"
                      + "- GATHERING: 회식")
          @RequestParam
          @NotNull VisitPurpose visitPurpose,
      @Parameter(
              description =
                  "동행자\n" + "- COUPLE: 연인\n" + "- FAMILY: 가족\n" + "- FRIEND: 친구\n" + "- ALONE: 혼자")
          @RequestParam
          @NotNull VisitWith visitWith,
      @Parameter(
              description =
                  "좋았던 점 (복수 선택 가능)\n"
                      + "- TASTY: 음식이 맛있어요\n"
                      + "- LARGE_PORTION: 양이 많아요\n"
                      + "- GOOD_VALUE: 가성비가 좋아요\n"
                      + "- GOOD_FOR_SOLO: 혼밥하기 좋아요\n"
                      + "- GOOD_FOR_GROUPS: 단체로 오기 좋아요\n"
                      + "- QUIET: 조용하고 좋아요")
          @RequestParam(required = false)
          List<GoodPoint> goodPoints,
      @Parameter(description = "텍스트 리뷰 (선택, 500자 이내)")
          @RequestParam(required = false)
          @Size(max = 500)
          String content,
      @Parameter(description = "리뷰 이미지 (선택, 최대 3개)")
          @RequestParam(required = false)
          @Size(max = 3, message = "이미지는 최대 3개까지 첨부할 수 있습니다.")
          List<MultipartFile> images) {
    ReviewCreateRequest request =
        ReviewCreateRequest.builder()
            .rating(rating)
            .benefitStatus(benefitStatus)
            .visitType(visitType)
            .waitTime(waitTime)
            .visitPurpose(visitPurpose)
            .visitWith(visitWith)
            .goodPoints(goodPoints)
            .content(content)
            .build();
    return BaseResponse.success(reviewService.createReview(storeId, email, request, images));
  }

  @Operation(
      summary = "[ 사용자 | 토큰 O | 매장 리뷰 수정 ]",
      security = @SecurityRequirement(name = "bearerAuth"),
      description =
          """
          **Purpose**
          - 본인이 작성한 리뷰를 수정합니다.

          **Parameters**
          - rating: 별점 (1~5)
          - benefitStatus: 군인 할인 혜택 여부 (RECEIVED / NOT_RECEIVED / PARTIAL) — SOLDIER 인증 유저 필수, MEMBER 생략 가능
          - visitType: 이용 방식 (WALK_IN / RESERVED / TAKEOUT_DELIVERY)
          - waitTime: 대기 시간 (IMMEDIATE / WITHIN_10_MIN / WITHIN_30_MIN / WITHIN_1_HOUR / OVER_1_HOUR)
          - visitPurpose: 방문 목적 (DATE / OUTING / OVERNIGHT_PASS / VACATION / GATHERING)
          - visitWith: 동행자 (COUPLE / FAMILY / FRIEND / ALONE)
          - goodPoints: 좋았던 점 (복수 선택, 선택 사항)
          - content: 텍스트 리뷰 (선택 사항, 500자 이내)

          **Parameters**
          - images: 새 리뷰 이미지 (선택, 최대 3개 / 전송 시 기존 이미지 교체)

          **Error**
          - STO4041: 존재하지 않는 매장
          - REV4041: 존재하지 않는 리뷰
          - REV4031: 본인 리뷰가 아닌 경우
          - AUTH4011: 토큰 미포함 또는 만료
          """)
  @PutMapping(
      value = "/{reviewId}",
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
  @PreAuthorize("isAuthenticated()")
  public BaseResponse<ReviewResponse> updateReview(
      @Parameter(description = "매장 ID") @PathVariable Long storeId,
      @Parameter(description = "리뷰 ID") @PathVariable Long reviewId,
      @AuthenticationPrincipal String email,
      @Parameter(description = "별점 (1~5)") @RequestParam @NotNull @Min(1) @Max(5) Integer rating,
      @Parameter(
              description =
                  "군인 할인 혜택 여부 (SOLDIER 인증 유저 필수 / MEMBER 생략 가능)\n"
                      + "- RECEIVED: 혜택 받음\n"
                      + "- NOT_RECEIVED: 혜택 받지 못함\n"
                      + "- PARTIAL: 일부 받음")
          @RequestParam(required = false)
          BenefitStatus benefitStatus,
      @Parameter(
              description =
                  "이용 방식\n"
                      + "- WALK_IN: 예약 없이 이용\n"
                      + "- RESERVED: 예약 후 이용\n"
                      + "- TAKEOUT_DELIVERY: 포장·배달 이용")
          @RequestParam
          @NotNull VisitType visitType,
      @Parameter(
              description =
                  "대기 시간\n"
                      + "- IMMEDIATE: 바로 입장\n"
                      + "- WITHIN_10_MIN: 10분 이내\n"
                      + "- WITHIN_30_MIN: 30분 이내\n"
                      + "- WITHIN_1_HOUR: 1시간 이내\n"
                      + "- OVER_1_HOUR: 1시간 이상")
          @RequestParam
          @NotNull WaitTime waitTime,
      @Parameter(
              description =
                  "방문 목적\n"
                      + "- DATE: 데이트\n"
                      + "- OUTING: 외출\n"
                      + "- OVERNIGHT_PASS: 외박\n"
                      + "- VACATION: 휴가\n"
                      + "- GATHERING: 회식")
          @RequestParam
          @NotNull VisitPurpose visitPurpose,
      @Parameter(
              description =
                  "동행자\n" + "- COUPLE: 연인\n" + "- FAMILY: 가족\n" + "- FRIEND: 친구\n" + "- ALONE: 혼자")
          @RequestParam
          @NotNull VisitWith visitWith,
      @Parameter(
              description =
                  "좋았던 점 (복수 선택 가능)\n"
                      + "- TASTY: 음식이 맛있어요\n"
                      + "- LARGE_PORTION: 양이 많아요\n"
                      + "- GOOD_VALUE: 가성비가 좋아요\n"
                      + "- GOOD_FOR_SOLO: 혼밥하기 좋아요\n"
                      + "- GOOD_FOR_GROUPS: 단체로 오기 좋아요\n"
                      + "- QUIET: 조용하고 좋아요")
          @RequestParam(required = false)
          List<GoodPoint> goodPoints,
      @Parameter(description = "텍스트 리뷰 (선택, 500자 이내)")
          @RequestParam(required = false)
          @Size(max = 500)
          String content,
      @Parameter(description = "새 리뷰 이미지 (선택, 최대 3개 / 전송 시 기존 이미지 교체)")
          @RequestParam(required = false)
          @Size(max = 3, message = "이미지는 최대 3개까지 첨부할 수 있습니다.")
          List<MultipartFile> images) {
    ReviewCreateRequest request =
        ReviewCreateRequest.builder()
            .rating(rating)
            .benefitStatus(benefitStatus)
            .visitType(visitType)
            .waitTime(waitTime)
            .visitPurpose(visitPurpose)
            .visitWith(visitWith)
            .goodPoints(goodPoints)
            .content(content)
            .build();
    return BaseResponse.success(
        reviewService.updateReview(storeId, reviewId, email, request, images));
  }

  @Operation(
      summary = "[ 사용자 | 토큰 O | 매장 리뷰 삭제 ]",
      security = @SecurityRequirement(name = "bearerAuth"),
      description =
          """
          **Purpose**
          - 본인이 작성한 리뷰를 삭제합니다.

          **Error**
          - STO4041: 존재하지 않는 매장
          - REV4041: 존재하지 않는 리뷰
          - REV4031: 본인 리뷰가 아닌 경우
          - AUTH4011: 토큰 미포함 또는 만료
          """)
  @DeleteMapping("/{reviewId}")
  @PreAuthorize("isAuthenticated()")
  public BaseResponse<Void> deleteReview(
      @Parameter(description = "매장 ID") @PathVariable Long storeId,
      @Parameter(description = "리뷰 ID") @PathVariable Long reviewId,
      @AuthenticationPrincipal String email) {
    reviewService.deleteReview(storeId, reviewId, email);
    return BaseResponse.success(null);
  }

  @Operation(
      summary = "[ 관리자 | 토큰 O | 리뷰 숨김 처리 ]",
      security = @SecurityRequirement(name = "bearerAuth"),
      description =
          """
          **Purpose**
          - 관리자가 특정 리뷰의 노출 상태를 변경합니다. (VISIBLE ↔ HIDDEN)

          **Authorization**
          - role이 ADMIN인 계정만 접근 가능합니다.

          **Error**
          - STO4041: 존재하지 않는 매장
          - REV4041: 존재하지 않는 리뷰
          - AUTH4011: 토큰 미포함 또는 만료
          - G003: 권한 없음 (403)
          """)
  @PatchMapping("/{reviewId}/status")
  @PreAuthorize("hasRole('ADMIN')")
  public BaseResponse<ReviewResponse> updateReviewStatus(
      @Parameter(description = "매장 ID") @PathVariable Long storeId,
      @Parameter(description = "리뷰 ID") @PathVariable Long reviewId,
      @Valid @RequestBody ReviewStatusRequest request) {
    return BaseResponse.success(reviewService.updateReviewStatus(storeId, reviewId, request));
  }
}
