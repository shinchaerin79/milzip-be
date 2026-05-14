package org.sku.milzip.domain.user.controller;

import java.util.List;

import org.sku.milzip.domain.user.dto.UserResponse;
import org.sku.milzip.domain.user.service.UserService;
import org.sku.milzip.global.common.BaseResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "User", description = "유저 API")
@RestController
@RequestMapping("/api/users")
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
}
