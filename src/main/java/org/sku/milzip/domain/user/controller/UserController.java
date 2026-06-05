package org.sku.milzip.domain.user.controller;

import java.util.List;

import org.sku.milzip.domain.review.dto.response.ReviewResponse;
import org.sku.milzip.domain.user.dto.response.FavoriteResponse;
import org.sku.milzip.domain.user.dto.response.UserResponse;
import org.sku.milzip.domain.user.service.UserService;
import org.sku.milzip.global.common.BaseResponse;
import org.sku.milzip.global.common.PageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "User", description = "유저 API")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @Operation(
      summary = "[ 사용자 | 토큰 O | 내 정보 조회 ]",
      security = @SecurityRequirement(name = "bearerAuth"),
      description =
          """
          **Purpose**
          - 현재 로그인한 사용자의 프로필 정보 조회

          **Returns**
          - id: 사용자 식별자
          - email: 이메일
          - nickname: 닉네임
          - role: 권한 (MEMBER / SOLDIER / ADMIN)
          - status: 계정 상태 (PENDING_EMAIL / ACTIVE / SUSPENDED)
          - authType: 가입 방식 (LOCAL / KAKAO)
          - militaryStatus: 군인 인증 상태 (NOT_VERIFIED / PENDING / VERIFIED)
          - temporaryPassword: 임시 비밀번호 사용 여부 — true이면 비밀번호 변경 필요

          **Error**
          - AUTH4011: 토큰 미포함 또는 만료
          """)
  @GetMapping("/me")
  public BaseResponse<UserResponse> getMyInfo(@AuthenticationPrincipal String email) {
    return BaseResponse.success(userService.getMyInfo(email));
  }

  @Operation(
      summary = "[ 관리자 | 토큰 O | 전체 사용자 조회 ]",
      security = @SecurityRequirement(name = "bearerAuth"),
      description =
          """
          **Purpose**
          - 관리자 전용: 가입된 모든 사용자 목록 조회

          **Authorization**
          - role이 ADMIN인 계정만 접근 가능
          - 일반 사용자 접근 시 403 Forbidden

          **Returns**
          - id, email, nickname, role, status, authType, militaryStatus, temporaryPassword

          **Error**
          - AUTH4011: 토큰 미포함 또는 만료
          - G003: 권한 없음 (403)
          """)
  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public BaseResponse<List<UserResponse>> getAllUsers() {
    return BaseResponse.success(userService.getAllUsers());
  }

  // 중복 확인 (비로그인 - 회원가입 시)

  @Operation(
      summary = "[ 전체 | 토큰 X | 이메일 중복 확인 ]",
      description = "**Error**\n- USR4092: 이미 사용 중인 이메일 (409)")
  @GetMapping("/email/availability")
  public BaseResponse<Void> checkEmail(
      @Parameter(description = "확인할 이메일", example = "test@milzip.com") @RequestParam String email) {
    userService.checkEmailDuplicate(email);
    return BaseResponse.success(null);
  }

  @Operation(
      summary = "[ 전체 | 토큰 X | 닉네임 중복 확인 ]",
      description = "**Error**\n- USR4093: 이미 사용 중인 닉네임 (409)")
  @GetMapping("/nickname/availability")
  public BaseResponse<Void> checkNickname(
      @Parameter(description = "확인할 닉네임", example = "밀집이") @RequestParam String nickname) {
    userService.checkNicknameDuplicate(nickname);
    return BaseResponse.success(null);
  }

  // 즐겨찾기

  @Operation(
      summary = "[ 사용자 | 토큰 O | 즐겨찾기 매장 목록 조회 ]",
      security = @SecurityRequirement(name = "bearerAuth"))
  @GetMapping("/favorites")
  @PreAuthorize("isAuthenticated()")
  public BaseResponse<List<FavoriteResponse>> getFavorites(@AuthenticationPrincipal String email) {
    return BaseResponse.success(userService.getFavorites(email));
  }

  @Operation(
      summary = "[ 사용자 | 토큰 O | 즐겨찾기 매장 추가 ]",
      security = @SecurityRequirement(name = "bearerAuth"),
      description = "**Error**\n- STO4041: 존재하지 않는 매장\n- USR4091: 이미 즐겨찾기에 추가된 매장")
  @PostMapping("/favorites/{storeId}")
  @PreAuthorize("isAuthenticated()")
  public BaseResponse<FavoriteResponse> addFavorite(
      @AuthenticationPrincipal String email,
      @Parameter(description = "매장 ID") @PathVariable Long storeId) {
    return BaseResponse.success(userService.addFavorite(email, storeId));
  }

  @Operation(
      summary = "[ 사용자 | 토큰 O | 즐겨찾기 매장 해제 ]",
      security = @SecurityRequirement(name = "bearerAuth"),
      description = "**Error**\n- USR4041: 즐겨찾기 내역 없음")
  @DeleteMapping("/favorites/{storeId}")
  @PreAuthorize("isAuthenticated()")
  public BaseResponse<Void> removeFavorite(
      @AuthenticationPrincipal String email,
      @Parameter(description = "매장 ID") @PathVariable Long storeId) {
    userService.removeFavorite(email, storeId);
    return BaseResponse.success(null);
  }

  // 내 리뷰

  @Operation(
      summary = "[ 사용자 | 토큰 O | 내 리뷰 목록 조회 ]",
      security = @SecurityRequirement(name = "bearerAuth"))
  @GetMapping("/reviews")
  @PreAuthorize("isAuthenticated()")
  public BaseResponse<PageResponse<ReviewResponse>> getMyReviews(
      @AuthenticationPrincipal String email,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    return BaseResponse.success(userService.getMyReviews(email, page, size));
  }
}
