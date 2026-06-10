package org.sku.milzip.domain.user.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sku.milzip.domain.auth.exception.AuthErrorCode;
import org.sku.milzip.domain.benefit.entity.Benefit;
import org.sku.milzip.domain.benefit.entity.Tmo;
import org.sku.milzip.domain.benefit.repository.BenefitRepository;
import org.sku.milzip.domain.benefit.repository.TmoRepository;
import org.sku.milzip.domain.review.dto.response.ReviewResponse;
import org.sku.milzip.domain.review.repository.ReviewRepository;
import org.sku.milzip.domain.store.entity.Store;
import org.sku.milzip.domain.store.exception.StoreErrorCode;
import org.sku.milzip.domain.store.repository.StoreRepository;
import org.sku.milzip.domain.user.dto.response.BenefitFavoriteResponse;
import org.sku.milzip.domain.user.dto.response.FavoriteResponse;
import org.sku.milzip.domain.user.dto.response.TmoFavoriteResponse;
import org.sku.milzip.domain.user.dto.response.UserResponse;
import org.sku.milzip.domain.user.entity.BenefitFavorite;
import org.sku.milzip.domain.user.entity.Favorite;
import org.sku.milzip.domain.user.entity.TmoFavorite;
import org.sku.milzip.domain.user.entity.User;
import org.sku.milzip.domain.user.exception.UserErrorCode;
import org.sku.milzip.domain.user.repository.BenefitFavoriteRepository;
import org.sku.milzip.domain.user.repository.FavoriteRepository;
import org.sku.milzip.domain.user.repository.TmoFavoriteRepository;
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
  private final BenefitRepository benefitRepository;
  private final TmoRepository tmoRepository;
  private final BenefitFavoriteRepository benefitFavoriteRepository;
  private final TmoFavoriteRepository tmoFavoriteRepository;
  private final ReviewRepository reviewRepository;
  private final Optional<S3AsyncService> s3AsyncService;
  private final Optional<S3Service> s3Service;

  @Transactional(readOnly = true)
  public UserResponse getMyInfo(String email) {
    log.debug("[UserService] 내 정보 조회 - email: {}", email);
    return userRepository
        .findByEmail(email)
        .map(UserResponse::from)
        .orElseThrow(
            () -> {
              log.warn("[UserService] 사용자 없음 - email: {}", email);
              return new CustomException(AuthErrorCode.USER_NOT_FOUND);
            });
  }

  @Transactional(readOnly = true)
  public List<UserResponse> getAllUsers() {
    log.debug("[UserService] 전체 유저 목록 조회");
    List<UserResponse> result = userRepository.findAll().stream().map(UserResponse::from).toList();
    log.debug("[UserService] 전체 유저 목록 조회 완료 - 총 {}명", result.size());
    return result;
  }

  // 즐겨찾기

  @Transactional(readOnly = true)
  public List<FavoriteResponse> getFavorites(String email) {
    log.debug("[UserService] 매장 즐겨찾기 목록 조회 - email: {}", email);
    User user = getUser(email);
    List<Favorite> favorites = favoriteRepository.findByUserIdWithStore(user.getId());
    List<Long> storeIds = favorites.stream().map(f -> f.getStore().getId()).toList();
    Map<Long, String> thumbnailMap = storeRepository.findThumbnailMapByStoreIds(storeIds);
    List<FavoriteResponse> result =
        favorites.stream()
            .map(f -> FavoriteResponse.from(f, thumbnailMap.get(f.getStore().getId())))
            .toList();
    log.debug("[UserService] 매장 즐겨찾기 목록 조회 완료 - userId: {}, {}건", user.getId(), result.size());
    return result;
  }

  @Transactional
  public FavoriteResponse addFavorite(String email, Long storeId) {
    log.info("[UserService] 매장 즐겨찾기 추가 - email: {}, storeId: {}", email, storeId);
    User user = getUser(email);
    Store store =
        storeRepository
            .findById(storeId)
            .orElseThrow(
                () -> {
                  log.warn("[UserService] 즐겨찾기 추가 실패 - 매장 없음, storeId: {}", storeId);
                  return new CustomException(StoreErrorCode.STORE_NOT_FOUND);
                });

    if (favoriteRepository.existsByUserIdAndStoreId(user.getId(), storeId)) {
      log.warn("[UserService] 즐겨찾기 추가 실패 - 이미 추가됨, userId: {}, storeId: {}", user.getId(), storeId);
      throw new CustomException(UserErrorCode.FAVORITE_ALREADY_EXISTS);
    }

    Favorite saved = favoriteRepository.save(Favorite.create(user, store));
    Map<Long, String> thumbnailMap = storeRepository.findThumbnailMapByStoreIds(List.of(storeId));
    FavoriteResponse response = FavoriteResponse.from(saved, thumbnailMap.get(storeId));
    log.info("[UserService] 매장 즐겨찾기 추가 완료 - userId: {}, storeId: {}", user.getId(), storeId);
    return response;
  }

  @Transactional
  public void removeFavorite(String email, Long storeId) {
    log.info("[UserService] 매장 즐겨찾기 해제 - email: {}, storeId: {}", email, storeId);
    User user = getUser(email);
    Favorite favorite =
        favoriteRepository
            .findByUserIdAndStoreId(user.getId(), storeId)
            .orElseThrow(
                () -> {
                  log.warn(
                      "[UserService] 즐겨찾기 해제 실패 - 즐겨찾기 없음, userId: {}, storeId: {}",
                      user.getId(),
                      storeId);
                  return new CustomException(UserErrorCode.FAVORITE_NOT_FOUND);
                });
    favoriteRepository.delete(favorite);
    log.info("[UserService] 매장 즐겨찾기 해제 완료 - userId: {}, storeId: {}", user.getId(), storeId);
  }

  // 혜택 즐겨찾기

  @Transactional(readOnly = true)
  public List<BenefitFavoriteResponse> getBenefitFavorites(String email) {
    log.debug("[UserService] 혜택 즐겨찾기 목록 조회 - email: {}", email);
    User user = getUser(email);
    List<BenefitFavoriteResponse> result =
        benefitFavoriteRepository.findByUserIdWithBenefit(user.getId()).stream()
            .map(BenefitFavoriteResponse::from)
            .toList();
    log.debug("[UserService] 혜택 즐겨찾기 목록 조회 완료 - userId: {}, {}건", user.getId(), result.size());
    return result;
  }

  @Transactional
  public BenefitFavoriteResponse addBenefitFavorite(String email, Long benefitId) {
    log.info("[UserService] 혜택 즐겨찾기 추가 - email: {}, benefitId: {}", email, benefitId);
    User user = getUser(email);
    Benefit benefit =
        benefitRepository
            .findById(benefitId)
            .orElseThrow(
                () -> {
                  log.warn("[UserService] 혜택 즐겨찾기 추가 실패 - 혜택 없음, benefitId: {}", benefitId);
                  return new CustomException(UserErrorCode.BENEFIT_FAVORITE_NOT_FOUND);
                });

    if (benefitFavoriteRepository.existsByUserIdAndBenefitId(user.getId(), benefitId)) {
      log.warn(
          "[UserService] 혜택 즐겨찾기 추가 실패 - 이미 추가됨, userId: {}, benefitId: {}",
          user.getId(),
          benefitId);
      throw new CustomException(UserErrorCode.BENEFIT_FAVORITE_ALREADY_EXISTS);
    }

    BenefitFavoriteResponse response =
        BenefitFavoriteResponse.from(
            benefitFavoriteRepository.save(BenefitFavorite.create(user, benefit)));
    log.info("[UserService] 혜택 즐겨찾기 추가 완료 - userId: {}, benefitId: {}", user.getId(), benefitId);
    return response;
  }

  @Transactional
  public void removeBenefitFavorite(String email, Long benefitId) {
    log.info("[UserService] 혜택 즐겨찾기 해제 - email: {}, benefitId: {}", email, benefitId);
    User user = getUser(email);
    BenefitFavorite bf =
        benefitFavoriteRepository
            .findByUserIdAndBenefitId(user.getId(), benefitId)
            .orElseThrow(
                () -> {
                  log.warn(
                      "[UserService] 혜택 즐겨찾기 해제 실패 - 즐겨찾기 없음, userId: {}, benefitId: {}",
                      user.getId(),
                      benefitId);
                  return new CustomException(UserErrorCode.BENEFIT_FAVORITE_NOT_FOUND);
                });
    benefitFavoriteRepository.delete(bf);
    log.info("[UserService] 혜택 즐겨찾기 해제 완료 - userId: {}, benefitId: {}", user.getId(), benefitId);
  }

  // TMO 즐겨찾기

  @Transactional(readOnly = true)
  public List<TmoFavoriteResponse> getTmoFavorites(String email) {
    log.debug("[UserService] TMO 즐겨찾기 목록 조회 - email: {}", email);
    User user = getUser(email);
    List<TmoFavoriteResponse> result =
        tmoFavoriteRepository.findByUserIdWithTmo(user.getId()).stream()
            .map(TmoFavoriteResponse::from)
            .toList();
    log.debug("[UserService] TMO 즐겨찾기 목록 조회 완료 - userId: {}, {}건", user.getId(), result.size());
    return result;
  }

  @Transactional
  public TmoFavoriteResponse addTmoFavorite(String email, Long tmoId) {
    log.info("[UserService] TMO 즐겨찾기 추가 - email: {}, tmoId: {}", email, tmoId);
    User user = getUser(email);
    Tmo tmo =
        tmoRepository
            .findById(tmoId)
            .orElseThrow(
                () -> {
                  log.warn("[UserService] TMO 즐겨찾기 추가 실패 - TMO 없음, tmoId: {}", tmoId);
                  return new CustomException(UserErrorCode.TMO_FAVORITE_NOT_FOUND);
                });

    if (tmoFavoriteRepository.existsByUserIdAndTmoId(user.getId(), tmoId)) {
      log.warn("[UserService] TMO 즐겨찾기 추가 실패 - 이미 추가됨, userId: {}, tmoId: {}", user.getId(), tmoId);
      throw new CustomException(UserErrorCode.TMO_FAVORITE_ALREADY_EXISTS);
    }

    TmoFavoriteResponse response =
        TmoFavoriteResponse.from(tmoFavoriteRepository.save(TmoFavorite.create(user, tmo)));
    log.info("[UserService] TMO 즐겨찾기 추가 완료 - userId: {}, tmoId: {}", user.getId(), tmoId);
    return response;
  }

  @Transactional
  public void removeTmoFavorite(String email, Long tmoId) {
    log.info("[UserService] TMO 즐겨찾기 해제 - email: {}, tmoId: {}", email, tmoId);
    User user = getUser(email);
    TmoFavorite tf =
        tmoFavoriteRepository
            .findByUserIdAndTmoId(user.getId(), tmoId)
            .orElseThrow(
                () -> {
                  log.warn(
                      "[UserService] TMO 즐겨찾기 해제 실패 - 즐겨찾기 없음, userId: {}, tmoId: {}",
                      user.getId(),
                      tmoId);
                  return new CustomException(UserErrorCode.TMO_FAVORITE_NOT_FOUND);
                });
    tmoFavoriteRepository.delete(tf);
    log.info("[UserService] TMO 즐겨찾기 해제 완료 - userId: {}, tmoId: {}", user.getId(), tmoId);
  }

  // 내 리뷰

  @Transactional(readOnly = true)
  public PageResponse<ReviewResponse> getMyReviews(String email, int page, int size) {
    log.debug("[UserService] 내 리뷰 목록 조회 - email: {}, page: {}, size: {}", email, page, size);
    User user = getUser(email);
    PageResponse<ReviewResponse> result =
        PageResponse.from(
            reviewRepository
                .findByUserId(
                    user.getId(),
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(ReviewResponse::from));
    log.debug(
        "[UserService] 내 리뷰 목록 조회 완료 - userId: {}, 총 {}건", user.getId(), result.getTotalElements());
    return result;
  }

  // 닉네임 수정

  @Transactional
  public UserResponse updateNickname(String email, String nickname) {
    log.info("[UserService] 닉네임 수정 - email: {}, newNickname: {}", email, nickname);
    User user = getUser(email);
    if (userRepository.existsByNickname(nickname)) {
      log.warn("[UserService] 닉네임 수정 실패 - 중복 닉네임: {}", nickname);
      throw new CustomException(UserErrorCode.NICKNAME_ALREADY_EXISTS);
    }
    user.updateNickname(nickname);
    log.info("[UserService] 닉네임 수정 완료 - userId: {}, nickname: {}", user.getId(), nickname);
    return UserResponse.from(user);
  }

  // 프로필 이미지

  @Transactional
  public UserResponse updateProfileImage(String email, MultipartFile profileImage) {
    log.info(
        "[UserService] 프로필 이미지 수정 - email: {}, fileSize: {}bytes", email, profileImage.getSize());
    User user = getUser(email);

    String oldImageUrl = user.getProfileImageUrl();
    String newImageUrl =
        s3AsyncService.map(s3 -> s3.uploadFile(PathName.PROFILE, profileImage)).orElse(null);

    if (newImageUrl != null) {
      user.updateProfileImage(newImageUrl);
      log.debug("[UserService] 프로필 이미지 업로드 완료 - userId: {}", user.getId());
      if (oldImageUrl != null) {
        s3Service.ifPresent(
            s3 -> {
              try {
                s3.deleteFile(s3.extractKeyNameFromUrl(oldImageUrl));
                log.debug("[UserService] 기존 프로필 이미지 삭제 완료 - userId: {}", user.getId());
              } catch (Exception e) {
                log.warn("[UserService] 기존 프로필 이미지 삭제 실패 - url: {}", oldImageUrl, e);
              }
            });
      }
    } else {
      log.warn("[UserService] 프로필 이미지 업로드 실패 (S3 미연동) - userId: {}", user.getId());
    }

    log.info("[UserService] 프로필 이미지 수정 완료 - userId: {}", user.getId());
    return UserResponse.from(user);
  }

  // 중복 확인

  /** 이메일 중복 확인 (비로그인 - 회원가입 시) */
  @Transactional(readOnly = true)
  public void checkEmailDuplicate(String email) {
    if (userRepository.existsByEmailIgnoreCase(email)) {
      log.warn("[UserService] 이메일 중복 - email: {}", email);
      throw new CustomException(UserErrorCode.EMAIL_ALREADY_EXISTS);
    }
  }

  /** 닉네임 중복 확인 (비로그인 - 회원가입 시) */
  @Transactional(readOnly = true)
  public void checkNicknameDuplicate(String nickname) {
    if (userRepository.existsByNickname(nickname)) {
      log.warn("[UserService] 닉네임 중복 - nickname: {}", nickname);
      throw new CustomException(UserErrorCode.NICKNAME_ALREADY_EXISTS);
    }
  }

  // Private helpers

  private User getUser(String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(
            () -> {
              log.warn("[UserService] 사용자 없음 - email: {}", email);
              return new CustomException(AuthErrorCode.USER_NOT_FOUND);
            });
  }
}
