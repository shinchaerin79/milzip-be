package org.sku.milzip.domain.user.service;

import java.util.List;

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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final StoreRepository storeRepository;
  private final FavoriteRepository favoriteRepository;
  private final ReviewRepository reviewRepository;

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

  // Private helpers

  private User getUser(String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
  }
}
