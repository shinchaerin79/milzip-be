package org.sku.milzip.domain.review.service;

import org.sku.milzip.domain.review.dto.request.ReviewCreateRequest;
import org.sku.milzip.domain.review.dto.request.ReviewStatusRequest;
import org.sku.milzip.domain.review.dto.response.ReviewResponse;
import org.sku.milzip.domain.review.entity.Review;
import org.sku.milzip.domain.review.entity.ReviewStatus;
import org.sku.milzip.domain.review.exception.ReviewErrorCode;
import org.sku.milzip.domain.review.repository.ReviewRepository;
import org.sku.milzip.domain.store.entity.Store;
import org.sku.milzip.domain.store.exception.StoreErrorCode;
import org.sku.milzip.domain.store.repository.StoreRepository;
import org.sku.milzip.domain.user.entity.User;
import org.sku.milzip.domain.user.entity.UserRole;
import org.sku.milzip.domain.user.repository.UserRepository;
import org.sku.milzip.global.common.PageResponse;
import org.sku.milzip.global.exception.CustomException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final StoreRepository storeRepository;
  private final UserRepository userRepository;

  /** 매장 리뷰 목록 조회 (VISIBLE 상태만, 최신순) */
  @Transactional(readOnly = true)
  public PageResponse<ReviewResponse> getReviews(Long storeId, int page, int size) {
    validateStore(storeId);
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    return PageResponse.from(
        reviewRepository
            .findByStoreIdAndStatus(storeId, ReviewStatus.VISIBLE, pageable)
            .map(ReviewResponse::from));
  }

  /** 리뷰 작성 (매장당 1인 1개) */
  @Transactional
  public ReviewResponse createReview(Long storeId, String email, ReviewCreateRequest request) {
    Store store = validateStore(storeId);
    User user = getUser(email);

    if (reviewRepository.existsByStoreIdAndUserId(storeId, user.getId())) {
      throw new CustomException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
    }

    // SOLDIER(군인 인증 완료)인 경우 혜택 여부 필수
    if (user.getRole() == UserRole.SOLDIER && request.getBenefitStatus() == null) {
      throw new CustomException(ReviewErrorCode.BENEFIT_STATUS_REQUIRED);
    }

    // MEMBER(일반 유저)인 경우 benefitStatus 무시 → null로 저장
    ReviewCreateRequest sanitized =
        user.getRole() == UserRole.SOLDIER
            ? request
            : ReviewCreateRequest.builder()
                .rating(request.getRating())
                .benefitStatus(null)
                .visitType(request.getVisitType())
                .waitTime(request.getWaitTime())
                .visitPurpose(request.getVisitPurpose())
                .visitWith(request.getVisitWith())
                .goodPoints(request.getGoodPoints())
                .content(request.getContent())
                .build();

    return ReviewResponse.from(reviewRepository.save(Review.create(store, user, sanitized)));
  }

  /** 리뷰 수정 (본인만) */
  @Transactional
  public ReviewResponse updateReview(
      Long storeId, Long reviewId, String email, ReviewCreateRequest request) {
    validateStore(storeId);
    User user = getUser(email);
    Review review = getReview(reviewId);

    if (!review.getUser().getId().equals(user.getId())) {
      throw new CustomException(ReviewErrorCode.REVIEW_FORBIDDEN);
    }

    // SOLDIER인 경우 혜택 여부 필수
    if (user.getRole() == UserRole.SOLDIER && request.getBenefitStatus() == null) {
      throw new CustomException(ReviewErrorCode.BENEFIT_STATUS_REQUIRED);
    }

    ReviewCreateRequest sanitized =
        user.getRole() == UserRole.SOLDIER
            ? request
            : ReviewCreateRequest.builder()
                .rating(request.getRating())
                .benefitStatus(null)
                .visitType(request.getVisitType())
                .waitTime(request.getWaitTime())
                .visitPurpose(request.getVisitPurpose())
                .visitWith(request.getVisitWith())
                .goodPoints(request.getGoodPoints())
                .content(request.getContent())
                .build();

    review.update(sanitized);
    return ReviewResponse.from(review);
  }

  /** 리뷰 삭제 (본인만) */
  @Transactional
  public void deleteReview(Long storeId, Long reviewId, String email) {
    validateStore(storeId);
    User user = getUser(email);
    Review review = getReview(reviewId);

    if (!review.getUser().getId().equals(user.getId())) {
      throw new CustomException(ReviewErrorCode.REVIEW_FORBIDDEN);
    }

    reviewRepository.delete(review);
  }

  /** 리뷰 숨김 처리 (관리자 전용) */
  @Transactional
  public ReviewResponse updateReviewStatus(
      Long storeId, Long reviewId, ReviewStatusRequest request) {
    validateStore(storeId);
    Review review = getReview(reviewId);
    review.updateStatus(request.getStatus());
    return ReviewResponse.from(review);
  }

  // ==============================
  // Private helpers
  // ==============================

  private Store validateStore(Long storeId) {
    return storeRepository
        .findById(storeId)
        .orElseThrow(() -> new CustomException(StoreErrorCode.STORE_NOT_FOUND));
  }

  private User getUser(String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(
            () ->
                new CustomException(
                    org.sku.milzip.domain.auth.exception.AuthErrorCode.USER_NOT_FOUND));
  }

  private Review getReview(Long reviewId) {
    return reviewRepository
        .findById(reviewId)
        .orElseThrow(() -> new CustomException(ReviewErrorCode.REVIEW_NOT_FOUND));
  }
}
