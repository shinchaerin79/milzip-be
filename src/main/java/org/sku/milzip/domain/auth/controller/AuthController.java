package org.sku.milzip.domain.auth.controller;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.sku.milzip.domain.auth.dto.request.LoginRequest;
import org.sku.milzip.domain.auth.dto.request.PasswordChangeRequest;
import org.sku.milzip.domain.auth.dto.request.SendVerificationEmailRequest;
import org.sku.milzip.domain.auth.dto.request.SignUpRequest;
import org.sku.milzip.domain.auth.dto.request.VerifyEmailRequest;
import org.sku.milzip.domain.auth.dto.response.TokenResponse;
import org.sku.milzip.domain.auth.entity.VerificationType;
import org.sku.milzip.domain.auth.service.AuthService;
import org.sku.milzip.domain.auth.service.KakaoAuthService;
import org.sku.milzip.global.common.BaseResponse;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Auth", description = "인증/인가 API")
@Validated
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final KakaoAuthService kakaoAuthService;

  @Operation(
      summary = "[ 비회원 | 토큰 X | 이메일 인증 코드 발송 ]",
      description =
          """
          **Purpose**
          - 회원가입 전 이메일 소유 여부 확인을 위한 인증 코드 발송
          - 동일 이메일로 재요청 시 기존 코드는 삭제되고 새 코드가 발송됩니다

          **Parameters**
          - email: 이메일 주소 (RFC 5322 형식, ex. user@example.com)

          **Returns**
          - message: "인증 코드가 발송되었습니다."

          **Note**
          - 인증 코드는 6자리 숫자이며 **5분간** 유효합니다
          - 코드 발송 후 반드시 `PATCH /api/auth/email-verifications` 로 인증을 완료해야 합니다
          """)
  @PostMapping("/email-verifications")
  public BaseResponse<Void> sendVerificationEmail(
      @Valid @RequestBody SendVerificationEmailRequest request) {
    authService.sendVerificationEmail(request, VerificationType.SIGNUP);
    return BaseResponse.success("인증 코드가 발송되었습니다.", null);
  }

  @Operation(
      summary = "[ 비회원 | 토큰 X | 이메일 인증 코드 확인 ]",
      description =
          """
          **Purpose**
          - 발송된 인증 코드 일치 여부 확인 및 이메일 인증 완료 처리

          **Parameters**
          - email: 인증 코드를 수신한 이메일 주소
          - code: 6자리 숫자 인증 코드

          **Returns**
          - message: "이메일 인증이 완료되었습니다."

          **Error**
          - AUTH4004: 코드 불일치
          - AUTH4005: 코드 만료 (5분 초과)
          """)
  @PatchMapping("/email-verifications")
  public BaseResponse<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
    authService.verifyEmail(request, VerificationType.SIGNUP);
    return BaseResponse.success("이메일 인증이 완료되었습니다.", null);
  }

  @Operation(
      summary = "[ 비회원 | 토큰 X | 회원가입 ]",
      description =
          """
          **Purpose**
          - 이메일 인증이 완료된 계정으로 회원가입

          **Parameters**
          - email: 이메일 인증이 완료된 주소
          - password: 8~20자, 영문·숫자·특수문자(!@#$%^&*) 각 1자 이상 포함 (ex. Password1!)
          - nickname: 2~20자

          **Returns**
          - message: "회원가입이 완료되었습니다."

          **Parameters**
          - profileImage: 프로필 이미지 (선택, 이미지 파일)

          **Error**
          - AUTH4002: 이미 가입된 이메일
          - AUTH4003: 이메일 인증 미완료
          """)
  @PostMapping(
      value = "/register",
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
  public BaseResponse<Void> register(
      @RequestParam @NotBlank @Email String email,
      @RequestParam
          @NotBlank
          @Size(min = 8, max = 20)
          @Pattern(
              regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*]).+$",
              message = "비밀번호는 영문, 숫자, 특수문자(!@#$%^&*)를 각 1자 이상 포함해야 합니다.")
          String password,
      @RequestParam @NotBlank @Size(min = 2, max = 20) String nickname,
      @RequestParam @NotBlank String name,
      @RequestParam(required = false) MultipartFile profileImage) {
    authService.signUp(new SignUpRequest(email, password, nickname, name), profileImage);
    return BaseResponse.success("회원가입이 완료되었습니다.", null);
  }

  @Operation(
      summary = "[ 비회원 | 토큰 X | 로그인 ]",
      description =
          """
          **Purpose**
          - 이메일·비밀번호 기반 로그인

          **Parameters**
          - email: 가입 시 사용한 이메일
          - password: 계정 비밀번호

          **Returns**
          - accessToken: Bearer 토큰 (유효기간 30분), Authorization 헤더에 담아 요청
          - refreshToken: HttpOnly 쿠키로 자동 설정 (유효기간 14일)

          **Error**
          - AUTH4001: 이메일 또는 비밀번호 불일치
          - AUTH4003: 이메일 인증 미완료
          """)
  @PostMapping("/login")
  public BaseResponse<TokenResponse> login(
      @Valid @RequestBody LoginRequest request, HttpServletResponse response) {
    return BaseResponse.success(authService.login(request, response));
  }

  @Operation(
      summary = "[ 비회원 | 토큰 X | 비밀번호 재설정 인증 코드 발송 ]",
      description =
          """
          **Purpose**
          - 비밀번호 재설정을 위한 이메일 인증 코드 발송

          **Parameters**
          - email: 가입 시 사용한 이메일

          **Returns**
          - message: "인증 코드가 발송되었습니다."

          **Note**
          - 인증 코드는 6자리 숫자이며 **5분간** 유효합니다
          - 코드 발송 후 반드시 `PATCH /api/auth/password-resets/verifications` 로 인증을 완료해야 합니다
          """)
  @PostMapping("/password-resets/verifications")
  public BaseResponse<Void> sendPasswordResetVerification(
      @Valid @RequestBody SendVerificationEmailRequest request) {
    authService.sendVerificationEmail(request, VerificationType.PASSWORD_RESET);
    return BaseResponse.success("인증 코드가 발송되었습니다.", null);
  }

  @Operation(
      summary = "[ 비회원 | 토큰 X | 비밀번호 재설정 인증 코드 확인 ]",
      description =
          """
          **Purpose**
          - 비밀번호 재설정용 인증 코드 확인

          **Parameters**
          - email: 인증 코드를 수신한 이메일
          - code: 6자리 숫자 인증 코드

          **Returns**
          - message: "이메일 인증이 완료되었습니다."

          **Error**
          - AUTH4004: 코드 불일치
          - AUTH4005: 코드 만료 (5분 초과)
          """)
  @PatchMapping("/password-resets/verifications")
  public BaseResponse<Void> verifyPasswordResetCode(
      @Valid @RequestBody VerifyEmailRequest request) {
    authService.verifyEmail(request, VerificationType.PASSWORD_RESET);
    return BaseResponse.success("이메일 인증이 완료되었습니다.", null);
  }

  @Operation(
      summary = "[ 비회원 | 토큰 X | 비밀번호 변경 ]",
      description =
          """
          **Purpose**
          - 이메일 인증 완료 후 새 비밀번호로 변경

          **Parameters**
          - email: 인증이 완료된 이메일
          - newPassword: 새 비밀번호 (8~20자, 영문·숫자·특수문자(!@#$%^&*) 각 1자 이상)

          **Returns**
          - message: "비밀번호가 변경되었습니다."

          **Error**
          - AUTH4003: 이메일 인증 미완료
          - AUTH4041: 존재하지 않는 이메일
          """)
  @PutMapping("/password-resets")
  public BaseResponse<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
    authService.changePassword(request);
    return BaseResponse.success("비밀번호가 변경되었습니다.", null);
  }

  @Operation(
      summary = "[ 사용자 | 토큰 O | 액세스 토큰 재발급 ]",
      description =
          """
          **Purpose**
          - 만료된 액세스 토큰을 리프레시 토큰으로 재발급

          **Parameters**
          - refreshToken: 로그인 시 발급된 HttpOnly 쿠키 (자동 전송)

          **Returns**
          - accessToken: 새로운 Bearer 토큰 (유효기간 30분)
          - refreshToken: 갱신된 리프레시 토큰 (쿠키 자동 갱신, 유효기간 14일)

          **Error**
          - AUTH4014: 리프레시 토큰 만료 → 재로그인 필요
          - AUTH4015: 유효하지 않은 리프레시 토큰
          """)
  @PostMapping("/tokens/refresh")
  public BaseResponse<TokenResponse> refreshTokens(
      HttpServletRequest request, HttpServletResponse response) {
    return BaseResponse.success(authService.reissueTokens(request, response));
  }

  @Operation(
      summary = "[ 비회원 | 토큰 X | 카카오 로그인 페이지로 이동 ]",
      description =
          """
          **Purpose**
          - 카카오 OAuth 인증 페이지로 리다이렉트

          **Returns**
          - 카카오 로그인 화면으로 302 리다이렉트
          """)
  @GetMapping("/kakao")
  public void kakaoLogin(HttpServletResponse response) throws IOException {
    response.sendRedirect(kakaoAuthService.buildAuthorizationUrl());
  }

  @Operation(
      summary = "[ 비회원 | 토큰 X | 카카오 로그인 콜백 ]",
      description =
          """
          **Purpose**
          - 카카오 인증 완료 후 콜백을 처리하여 JWT 발급
          - 최초 로그인 시 자동으로 회원가입 후 로그인 처리

          **Parameters**
          - code: 카카오 인가 코드 (쿼리 파라미터, 카카오가 자동 전달)

          **Returns**
          - accessToken: Bearer 토큰 (쿠키 및 응답 바디)
          - 프론트엔드 URL로 302 리다이렉트

          **Error**
          - AUTH5003: 카카오 API 호출 실패
          """)
  @GetMapping("/kakao/callback")
  public void kakaoCallback(
      @RequestParam String code, HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    authService.kakaoLogin(code, response);
    response.sendRedirect(kakaoAuthService.buildFrontendRedirectUrl());
  }
}
