package org.sku.milzip.domain.user.service;

import java.util.List;
import java.util.Optional;

import org.sku.milzip.domain.auth.exception.AuthErrorCode;
import org.sku.milzip.domain.review.dto.response.ReviewResponse;
import org.sku.milzip.domain.review.repository.ReviewRepository;
import org.sku.milzip.domain.store.entity.Store;
import org.sku.milzip.domain.store.exception.StoreErrorCode;
import org.sku.milzip.domain.store.repository.StoreRepository;
import org.sku.milzip.domain.user.dto.response.FavoriteResponse;
import org.sku.milzip.domain.user.dto.response.UserResponse;
import org.sku.milzip.domain.user.entity.Favorite;
import org.sku.milzip.domain.user.entity.User;
import org.sku.milzip.domain.user.exception.UserErrorCode;
import org.sku.milzip.domain.user.repository.FavoriteRepository;
import org.sku.milzip.domain.user.repository.UserRepository;
import org.sku.milzip.global.common.PageResponse;
import org.sku.milzip.global.exception.CustomException;
import org.sku.milzip.global.s3.enums.PathName;
import org.sku.milzip.global.s3.service.S3AsyncService;
import org.sku.milzip.global.s3.service.S3Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final StoreRepository storeRepository;
  private final FavoriteRepository favoriteRepository;
  private final ReviewRepository reviewRepository;
  private final Optional<S3AsyncService> s3AsyncService;
  private final Optional<S3Service> s3Service;

  @Transactional(readOnly = true)
  public UserResponse getMyInfo(String email) {
    return userRepository
        .findByEmail(email)
        .map(UserResponse::from)
        .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
  }

  @Transactional(readOnly = true)
  public List<UserResponse> getAllUsers() {
    return userRepository.findAll().stream().map(UserResponse::from).toList();
  }

  // 즐겨찾기

  @Transactional(readOnly = true)
  public List<FavoriteResponse> getFavorites(String email) {
    User user = getUser(email);
    return favoriteRepository.findByUserIdWithStore(user.getId()).stream()
        .map(FavoriteResponse::from)
        .toList();
  }

  @Transactional
  public FavoriteResponse addFavorite(String email, Long storeId) {
    User user = getUser(email);
    Store store =
        storeRepository
            .findById(storeId)
            .orElseThrow(() -> new CustomException(StoreErrorCode.STORE_NOT_FOUND));

    if (favoriteRepository.existsByUserIdAndStoreId(user.getId(), storeId)) {
      throw new CustomException(UserErrorCode.FAVORITE_ALREADY_EXISTS);
    }

    return FavoriteResponse.from(favoriteRepository.save(Favorite.create(user, store)));
  }

  @Transactional
  public void removeFavorite(String email, Long storeId) {
    User user = getUser(email);
    Favorite favorite =
        favoriteRepository
            .findByUserIdAndStoreId(user.getId(), storeId)
            .orElseThrow(() -> new CustomException(UserErrorCode.FAVORITE_NOT_FOUND));
    favoriteRepository.delete(favorite);
  }

  // 내 리뷰

  @Transactional(readOnly = true)
  public PageResponse<ReviewResponse> getMyReviews(String email, int page, int size) {
    User user = getUser(email);
    return PageResponse.from(
        reviewRepository
            .findByUserId(
                user.getId(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
            .map(ReviewResponse::from));
  }

  // 프로필 이미지

  @Transactional
  public UserResponse updateProfileImage(String email, MultipartFile profileImage) {
    User user = getUser(email);

    String oldImageUrl = user.getProfileImageUrl();
    String newImageUrl =
        s3AsyncService.map(s3 -> s3.uploadFile(PathName.PROFILE, profileImage)).orElse(null);

    if (newImageUrl != null) {
      user.updateProfileImage(newImageUrl);
      if (oldImageUrl != null) {
        s3Service.ifPresent(
            s3 -> {
              try {
                s3.deleteFile(s3.extractKeyNameFromUrl(oldImageUrl));
              } catch (Exception e) {
                log.warn("[UserService] 기존 프로필 이미지 삭제 실패 - url: {}", oldImageUrl, e);
              }
            });
      }
    }

    return UserResponse.from(user);
  }

  // 중복 확인

  /** 이메일 중복 확인 (비로그인 - 회원가입 시) */
  @Transactional(readOnly = true)
  public void checkEmailDuplicate(String email) {
    if (userRepository.existsByEmailIgnoreCase(email)) {
      throw new CustomException(UserErrorCode.EMAIL_ALREADY_EXISTS);
    }
  }

  /** 닉네임 중복 확인 (비로그인 - 회원가입 시) */
  @Transactional(readOnly = true)
  public void checkNicknameDuplicate(String nickname) {
    if (userRepository.existsByNickname(nickname)) {
      throw new CustomException(UserErrorCode.NICKNAME_ALREADY_EXISTS);
    }
  }

  // Private helpers

  private User getUser(String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
  }
}
