package org.sku.milzip.domain.review.service;

import java.util.List;
import java.util.Optional;

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
import org.sku.milzip.global.s3.enums.PathName;
import org.sku.milzip.global.s3.service.S3AsyncService;
import org.sku.milzip.global.s3.service.S3Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final StoreRepository storeRepository;
  private final UserRepository userRepository;
  private final Optional<S3AsyncService> s3AsyncService;
  private final Optional<S3Service> s3Service;

  /** 리뷰 단건 조회 */
  @Transactional(readOnly = true)
  public ReviewResponse getReview(Long storeId, Long reviewId) {
    validateStore(storeId);
    return reviewRepository
        .findByStoreIdAndId(storeId, reviewId)
        .map(ReviewResponse::from)
        .orElseThrow(() -> new CustomException(ReviewErrorCode.REVIEW_NOT_FOUND));
  }

  /** 내 리뷰 목록 조회 */
  @Transactional(readOnly = true)
  public PageResponse<ReviewResponse> getMyReviews(String email, int page, int size) {
    User user = getUser(email);
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    return PageResponse.from(
        reviewRepository.findByUserId(user.getId(), pageable).map(ReviewResponse::from));
  }

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

  /** 리뷰 작성 */
  @Transactional
  public ReviewResponse createReview(
      Long storeId, String email, ReviewCreateRequest request, List<MultipartFile> images) {
    Store store = validateStore(storeId);
    User user = getUser(email);

    // 같은 영수증으로 중복 작성 방지
    String receiptIdentifier = request.getReceiptIdentifier();
    if (receiptIdentifier != null
        && reviewRepository.existsByReceiptIdentifier(receiptIdentifier)) {
      throw new CustomException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
    }

    // SOLDIER(군인 인증 완료)인 경우 혜택 여부 필수
    if (user.getRole() == UserRole.SOLDIER && request.getBenefitStatus() == null) {
      throw new CustomException(ReviewErrorCode.BENEFIT_STATUS_REQUIRED);
    }

    Review review = reviewRepository.save(Review.create(store, user, sanitize(user, request)));
    List<String> imageUrls = uploadImages(images);
    review.updateImages(imageUrls);
    return ReviewResponse.from(review);
  }

  /** 리뷰 수정 (본인만) */
  @Transactional
  public ReviewResponse updateReview(
      Long storeId,
      Long reviewId,
      String email,
      ReviewCreateRequest request,
      List<MultipartFile> images) {
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

    review.update(sanitize(user, request));

    // images 파라미터가 전달된 경우에만 교체
    if (images != null) {
      deleteImages(review.getImageUrls());
      review.updateImages(uploadImages(images));
    }

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

    deleteImages(review.getImageUrls());
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

  private List<String> uploadImages(List<MultipartFile> images) {
    if (images == null || images.isEmpty()) {
      return List.of();
    }
    return s3AsyncService.map(s3 -> s3.uploadFiles(PathName.REVIEW, images)).orElse(List.of());
  }

  private void deleteImages(List<String> imageUrls) {
    if (imageUrls == null || imageUrls.isEmpty()) {
      return;
    }
    s3Service.ifPresent(
        s3 ->
            imageUrls.forEach(
                url -> {
                  try {
                    s3.deleteFile(s3.extractKeyNameFromUrl(url));
                  } catch (Exception e) {
                    log.warn("[ReviewService] 이미지 삭제 실패 - url: {}", url, e);
                  }
                }));
  }

  // MEMBER인 경우 benefitStatus 무시 (null 처리)
  private ReviewCreateRequest sanitize(User user, ReviewCreateRequest request) {
    if (user.getRole() == UserRole.SOLDIER) {
      return request;
    }
    return ReviewCreateRequest.builder()
        .rating(request.getRating())
        .benefitStatus(null)
        .visitType(request.getVisitType())
        .waitTime(request.getWaitTime())
        .visitPurpose(request.getVisitPurpose())
        .visitWith(request.getVisitWith())
        .goodPoints(request.getGoodPoints())
        .content(request.getContent())
        .build();
  }

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
