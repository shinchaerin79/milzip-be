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
    log.debug("[ReviewService] 리뷰 단건 조회 - storeId: {}, reviewId: {}", storeId, reviewId);
    validateStore(storeId);
    ReviewResponse response =
        reviewRepository
            .findByStoreIdAndId(storeId, reviewId)
            .map(ReviewResponse::from)
            .orElseThrow(
                () -> {
                  log.warn("[ReviewService] 리뷰 없음 - storeId: {}, reviewId: {}", storeId, reviewId);
                  return new CustomException(ReviewErrorCode.REVIEW_NOT_FOUND);
                });
    log.debug(
        "[ReviewService] 리뷰 단건 조회 완료 - reviewId: {}, rating: {}", reviewId, response.getRating());
    return response;
  }

  /** 내 리뷰 목록 조회 */
  @Transactional(readOnly = true)
  public PageResponse<ReviewResponse> getMyReviews(String email, int page, int size) {
    log.debug("[ReviewService] 내 리뷰 목록 조회 - email: {}, page: {}, size: {}", email, page, size);
    User user = getUser(email);
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    PageResponse<ReviewResponse> result =
        PageResponse.from(
            reviewRepository.findByUserId(user.getId(), pageable).map(ReviewResponse::from));
    log.debug(
        "[ReviewService] 내 리뷰 목록 조회 완료 - userId: {}, 총 {}건",
        user.getId(),
        result.getTotalElements());
    return result;
  }

  /** 매장 리뷰 목록 조회 (VISIBLE 상태만, 최신순) */
  @Transactional(readOnly = true)
  public PageResponse<ReviewResponse> getReviews(Long storeId, int page, int size) {
    log.debug("[ReviewService] 매장 리뷰 목록 조회 - storeId: {}, page: {}, size: {}", storeId, page, size);
    validateStore(storeId);
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    PageResponse<ReviewResponse> result =
        PageResponse.from(
            reviewRepository
                .findByStoreIdAndStatus(storeId, ReviewStatus.VISIBLE, pageable)
                .map(ReviewResponse::from));
    log.debug(
        "[ReviewService] 매장 리뷰 목록 조회 완료 - storeId: {}, 총 {}건", storeId, result.getTotalElements());
    return result;
  }

  /** 리뷰 작성 */
  @Transactional
  public ReviewResponse createReview(
      Long storeId, String email, ReviewCreateRequest request, List<MultipartFile> images) {
    log.info(
        "[ReviewService] 리뷰 작성 시작 - storeId: {}, email: {}, rating: {}, imageCount: {}",
        storeId,
        email,
        request.getRating(),
        images == null ? 0 : images.size());

    Store store = validateStore(storeId);
    User user = getUser(email);

    // 같은 영수증으로 중복 작성 방지
    String receiptIdentifier = request.getReceiptIdentifier();
    if (receiptIdentifier != null
        && reviewRepository.existsByReceiptIdentifier(receiptIdentifier)) {
      log.warn(
          "[ReviewService] 중복 영수증으로 리뷰 작성 시도 - email: {}, receiptIdentifier: {}",
          email,
          receiptIdentifier);
      throw new CustomException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
    }

    // SOLDIER(군인 인증 완료)인 경우 혜택 여부 필수
    if (user.getRole() == UserRole.SOLDIER && request.getBenefitStatus() == null) {
      log.warn("[ReviewService] SOLDIER 유저 혜택 여부 미입력 - userId: {}, email: {}", user.getId(), email);
      throw new CustomException(ReviewErrorCode.BENEFIT_STATUS_REQUIRED);
    }

    Review review = reviewRepository.save(Review.create(store, user, sanitize(user, request)));
    log.debug("[ReviewService] 리뷰 저장 완료 - reviewId: {}", review.getId());

    List<String> imageUrls = uploadImages(images);
    if (!imageUrls.isEmpty()) {
      log.debug(
          "[ReviewService] 리뷰 이미지 업로드 완료 - reviewId: {}, 업로드 {}장",
          review.getId(),
          imageUrls.size());
    }
    review.updateImages(imageUrls);

    log.info(
        "[ReviewService] 리뷰 작성 완료 - reviewId: {}, storeId: {}, userId: {}, role: {}",
        review.getId(),
        storeId,
        user.getId(),
        user.getRole());
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
    log.info(
        "[ReviewService] 리뷰 수정 시작 - storeId: {}, reviewId: {}, email: {}, newRating: {}",
        storeId,
        reviewId,
        email,
        request.getRating());

    validateStore(storeId);
    User user = getUser(email);
    Review review = getReview(reviewId);

    if (!review.getUser().getId().equals(user.getId())) {
      log.warn(
          "[ReviewService] 리뷰 수정 권한 없음 - reviewId: {}, 요청자 userId: {}, 작성자 userId: {}",
          reviewId,
          user.getId(),
          review.getUser().getId());
      throw new CustomException(ReviewErrorCode.REVIEW_FORBIDDEN);
    }

    // SOLDIER인 경우 혜택 여부 필수
    if (user.getRole() == UserRole.SOLDIER && request.getBenefitStatus() == null) {
      log.warn(
          "[ReviewService] SOLDIER 유저 혜택 여부 미입력 (수정) - userId: {}, reviewId: {}",
          user.getId(),
          reviewId);
      throw new CustomException(ReviewErrorCode.BENEFIT_STATUS_REQUIRED);
    }

    int previousRating = review.getRating();
    review.update(sanitize(user, request));
    log.debug(
        "[ReviewService] 리뷰 내용 수정 - reviewId: {}, 별점 변경: {} → {}",
        reviewId,
        previousRating,
        request.getRating());

    // images 파라미터가 전달된 경우에만 교체
    if (images != null) {
      int previousImageCount = review.getImageUrls().size();
      deleteImages(review.getImageUrls());
      List<String> newImageUrls = uploadImages(images);
      review.updateImages(newImageUrls);
      log.debug(
          "[ReviewService] 리뷰 이미지 교체 - reviewId: {}, 이전 {}장 → 새 {}장",
          reviewId,
          previousImageCount,
          newImageUrls.size());
    }

    log.info("[ReviewService] 리뷰 수정 완료 - reviewId: {}, userId: {}", reviewId, user.getId());
    return ReviewResponse.from(review);
  }

  /** 리뷰 삭제 (본인만) */
  @Transactional
  public void deleteReview(Long storeId, Long reviewId, String email) {
    log.info(
        "[ReviewService] 리뷰 삭제 시작 - storeId: {}, reviewId: {}, email: {}",
        storeId,
        reviewId,
        email);

    validateStore(storeId);
    User user = getUser(email);
    Review review = getReview(reviewId);

    if (!review.getUser().getId().equals(user.getId())) {
      log.warn(
          "[ReviewService] 리뷰 삭제 권한 없음 - reviewId: {}, 요청자 userId: {}, 작성자 userId: {}",
          reviewId,
          user.getId(),
          review.getUser().getId());
      throw new CustomException(ReviewErrorCode.REVIEW_FORBIDDEN);
    }

    int imageCount = review.getImageUrls().size();
    deleteImages(review.getImageUrls());
    reviewRepository.delete(review);

    log.info(
        "[ReviewService] 리뷰 삭제 완료 - reviewId: {}, storeId: {}, userId: {}, 삭제된 이미지: {}장",
        reviewId,
        storeId,
        user.getId(),
        imageCount);
  }

  /** 리뷰 숨김 처리 (관리자 전용) */
  @Transactional
  public ReviewResponse updateReviewStatus(
      Long storeId, Long reviewId, ReviewStatusRequest request) {
    log.info(
        "[ReviewService] 리뷰 상태 변경 시작 - storeId: {}, reviewId: {}, 요청 상태: {}",
        storeId,
        reviewId,
        request.getStatus());

    validateStore(storeId);
    Review review = getReview(reviewId);
    ReviewStatus previousStatus = review.getStatus();
    review.updateStatus(request.getStatus());

    log.info(
        "[ReviewService] 리뷰 상태 변경 완료 - reviewId: {}, 상태 변경: {} → {}",
        reviewId,
        previousStatus,
        request.getStatus());
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
