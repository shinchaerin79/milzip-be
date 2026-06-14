package org.sku.milzip.domain.auth.service;

import java.util.Arrays;
import java.util.Optional;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.sku.milzip.domain.auth.dto.request.LoginRequest;
import org.sku.milzip.domain.auth.dto.request.PasswordChangeRequest;
import org.sku.milzip.domain.auth.dto.request.SendVerificationEmailRequest;
import org.sku.milzip.domain.auth.dto.request.SignUpRequest;
import org.sku.milzip.domain.auth.dto.request.VerifyEmailRequest;
import org.sku.milzip.domain.auth.dto.response.KakaoLoginResult;
import org.sku.milzip.domain.auth.dto.response.KakaoUserInfoResponse;
import org.sku.milzip.domain.auth.dto.response.TokenResponse;
import org.sku.milzip.domain.auth.entity.EmailVerification;
import org.sku.milzip.domain.auth.entity.RefreshToken;
import org.sku.milzip.domain.auth.entity.VerificationType;
import org.sku.milzip.domain.auth.exception.AuthErrorCode;
import org.sku.milzip.domain.auth.repository.EmailVerificationRepository;
import org.sku.milzip.domain.auth.repository.RefreshTokenRepository;
import org.sku.milzip.domain.email.service.EmailService;
import org.sku.milzip.domain.user.entity.User;
import org.sku.milzip.domain.user.entity.UserStatus;
import org.sku.milzip.domain.user.repository.UserRepository;
import org.sku.milzip.global.config.properties.JwtProperties;
import org.sku.milzip.global.exception.CustomException;
import org.sku.milzip.global.s3.enums.PathName;
import org.sku.milzip.global.s3.service.S3AsyncService;
import org.sku.milzip.global.security.CustomUserDetails;
import org.sku.milzip.global.security.jwt.JwtProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

  private static final String REFRESH_TOKEN_COOKIE = "refreshToken";
  private static final String ACCESS_TOKEN_COOKIE = JwtProvider.ACCESS_TOKEN_COOKIE;

  private final UserRepository userRepository;
  private final EmailVerificationRepository emailVerificationRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final EmailService emailService;
  private final KakaoAuthService kakaoAuthService;
  private final JwtProvider jwtProvider;
  private final JwtProperties jwtProperties;
  private final PasswordEncoder passwordEncoder;
  private final Optional<S3AsyncService> s3AsyncService;

  @Transactional
  public void sendVerificationEmail(SendVerificationEmailRequest request, VerificationType type) {
    log.info("[AuthService] 이메일 인증 코드 발송 요청 - email: {}, type: {}", request.email(), type);

    emailVerificationRepository.deleteByEmailAndType(request.email(), type);
    String code = emailService.generateVerificationCode();
    emailVerificationRepository.save(EmailVerification.create(request.email(), code, type));
    emailService.sendVerificationEmail(request.email(), code);

    log.info("[AuthService] 이메일 인증 코드 발송 완료 - email: {}", request.email());
  }

  @Transactional
  public void verifyEmail(VerifyEmailRequest request, VerificationType type) {
    log.info("[AuthService] 이메일 인증 코드 확인 요청 - email: {}, type: {}", request.email(), type);

    EmailVerification verification =
        emailVerificationRepository
            .findTopByEmailAndTypeOrderByIdDesc(request.email(), type)
            .orElseThrow(
                () -> {
                  log.warn("[AuthService] 이메일 인증 실패 - 인증 코드 없음, email: {}", request.email());
                  return new CustomException(AuthErrorCode.VERIFICATION_CODE_INVALID);
                });

    if (verification.isExpired()) {
      log.warn("[AuthService] 이메일 인증 실패 - 코드 만료, email: {}", request.email());
      throw new CustomException(AuthErrorCode.VERIFICATION_CODE_EXPIRED);
    }
    if (!verification.getCode().equals(request.code())) {
      log.warn("[AuthService] 이메일 인증 실패 - 코드 불일치, email: {}", request.email());
      throw new CustomException(AuthErrorCode.VERIFICATION_CODE_INVALID);
    }

    verification.verify();
    log.info("[AuthService] 이메일 인증 완료 - email: {}", request.email());
  }

  @Transactional
  public void signUp(SignUpRequest request, MultipartFile profileImage) {
    log.info(
        "[AuthService] 회원가입 요청 - email: {}, nickname: {}", request.email(), request.nickname());

    if (userRepository.existsByEmail(request.email())) {
      log.warn("[AuthService] 회원가입 실패 - 중복 이메일: {}", request.email());
      throw new CustomException(AuthErrorCode.DUPLICATE_EMAIL);
    }

    EmailVerification verification =
        emailVerificationRepository
            .findTopByEmailAndTypeOrderByIdDesc(request.email(), VerificationType.SIGNUP)
            .orElseThrow(
                () -> {
                  log.warn("[AuthService] 회원가입 실패 - 이메일 인증 기록 없음, email: {}", request.email());
                  return new CustomException(AuthErrorCode.EMAIL_NOT_VERIFIED);
                });

    if (!verification.isVerified()) {
      log.warn("[AuthService] 회원가입 실패 - 이메일 인증 미완료, email: {}", request.email());
      throw new CustomException(AuthErrorCode.EMAIL_NOT_VERIFIED);
    }

    User user =
        userRepository.save(
            User.ofLocal(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.nickname(),
                request.name()));
    user.activateEmail();

    if (profileImage != null && !profileImage.isEmpty()) {
      String imageUrl =
          s3AsyncService.map(s3 -> s3.uploadFile(PathName.PROFILE, profileImage)).orElse(null);
      if (imageUrl != null) {
        user.updateProfileImage(imageUrl);
      }
    }

    emailVerificationRepository.deleteByEmailAndType(request.email(), VerificationType.SIGNUP);

    log.info("[AuthService] 회원가입 완료 - userId: {}, email: {}", user.getId(), user.getEmail());
  }

  @Transactional
  public TokenResponse login(LoginRequest request, HttpServletResponse response) {
    log.info("[AuthService] 로그인 요청 - email: {}", request.email());

    User user =
        userRepository
            .findByEmail(request.email())
            .orElseThrow(
                () -> {
                  log.warn("[AuthService] 로그인 실패 - 존재하지 않는 이메일: {}", request.email());
                  return new CustomException(AuthErrorCode.INVALID_CREDENTIALS);
                });

    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      log.warn("[AuthService] 로그인 실패 - 비밀번호 불일치, email: {}", request.email());
      throw new CustomException(AuthErrorCode.INVALID_CREDENTIALS);
    }

    if (user.getStatus() == UserStatus.PENDING_EMAIL) {
      log.warn("[AuthService] 로그인 실패 - 이메일 인증 미완료, email: {}", request.email());
      throw new CustomException(AuthErrorCode.EMAIL_NOT_VERIFIED);
    }

    CustomUserDetails userDetails = new CustomUserDetails(user);
    String accessToken =
        jwtProvider.generateAccessToken(user.getEmail(), userDetails.getAuthorities());
    String refreshToken = jwtProvider.generateRefreshToken(user.getEmail());

    refreshTokenRepository
        .findByMemberId(user.getId())
        .ifPresentOrElse(
            rt -> {
              rt.rotate(refreshToken, jwtProperties.getRefreshExpiration());
              log.info("[AuthService] 리프레시 토큰 갱신 - userId: {}", user.getId());
            },
            () -> {
              refreshTokenRepository.save(
                  RefreshToken.create(
                      user.getId(), refreshToken, jwtProperties.getRefreshExpiration()));
              log.info("[AuthService] 리프레시 토큰 신규 발급 - userId: {}", user.getId());
            });

    setAccessTokenCookie(response, accessToken);
    setRefreshTokenCookie(response, refreshToken);
    log.info("[AuthService] 로그인 완료 - userId: {}, email: {}", user.getId(), user.getEmail());
    return new TokenResponse(accessToken);
  }

  @Transactional
  public TokenResponse reissueTokens(HttpServletRequest request, HttpServletResponse response) {
    log.info("[AuthService] 토큰 재발급 요청");

    String token = extractRefreshTokenCookie(request);

    RefreshToken refreshToken =
        refreshTokenRepository
            .findByToken(token)
            .orElseThrow(
                () -> {
                  log.warn("[AuthService] 토큰 재발급 실패 - 유효하지 않은 리프레시 토큰");
                  return new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
                });

    if (refreshToken.isExpired()) {
      refreshTokenRepository.delete(refreshToken);
      log.warn("[AuthService] 토큰 재발급 실패 - 만료된 리프레시 토큰, memberId: {}", refreshToken.getMemberId());
      throw new CustomException(AuthErrorCode.EXPIRED_REFRESH_TOKEN);
    }

    User user =
        userRepository
            .findById(refreshToken.getMemberId())
            .orElseThrow(
                () -> {
                  log.warn(
                      "[AuthService] 토큰 재발급 실패 - 존재하지 않는 사용자, memberId: {}",
                      refreshToken.getMemberId());
                  return new CustomException(AuthErrorCode.USER_NOT_FOUND);
                });

    CustomUserDetails userDetails = new CustomUserDetails(user);
    String newAccessToken =
        jwtProvider.generateAccessToken(user.getEmail(), userDetails.getAuthorities());
    String newRefreshToken = jwtProvider.generateRefreshToken(user.getEmail());

    refreshToken.rotate(newRefreshToken, jwtProperties.getRefreshExpiration());
    setAccessTokenCookie(response, newAccessToken);
    setRefreshTokenCookie(response, newRefreshToken);

    log.info("[AuthService] 토큰 재발급 완료 - userId: {}", user.getId());
    return new TokenResponse(newAccessToken);
  }

  @Transactional
  public void changePassword(PasswordChangeRequest request) {
    log.info("[AuthService] 비밀번호 변경 요청 - email: {}", request.email());

    EmailVerification verification =
        emailVerificationRepository
            .findTopByEmailAndTypeOrderByIdDesc(request.email(), VerificationType.PASSWORD_RESET)
            .orElseThrow(
                () -> {
                  log.warn("[AuthService] 비밀번호 변경 실패 - 이메일 인증 기록 없음, email: {}", request.email());
                  return new CustomException(AuthErrorCode.EMAIL_NOT_VERIFIED);
                });

    if (!verification.isVerified()) {
      log.warn("[AuthService] 비밀번호 변경 실패 - 이메일 인증 미완료, email: {}", request.email());
      throw new CustomException(AuthErrorCode.EMAIL_NOT_VERIFIED);
    }

    User user =
        userRepository
            .findByEmail(request.email())
            .orElseThrow(
                () -> {
                  log.warn("[AuthService] 비밀번호 변경 실패 - 존재하지 않는 이메일: {}", request.email());
                  return new CustomException(AuthErrorCode.USER_NOT_FOUND);
                });

    user.changePassword(passwordEncoder.encode(request.newPassword()));
    emailVerificationRepository.deleteByEmailAndType(
        request.email(), VerificationType.PASSWORD_RESET);

    log.info("[AuthService] 비밀번호 변경 완료 - userId: {}, email: {}", user.getId(), user.getEmail());
  }

  @Transactional
  public void logout(HttpServletRequest request, HttpServletResponse response) {
    log.info("[AuthService] 로그아웃 요청");

    if (request.getCookies() != null) {
      Arrays.stream(request.getCookies())
          .filter(c -> REFRESH_TOKEN_COOKIE.equals(c.getName()))
          .map(Cookie::getValue)
          .findFirst()
          .ifPresent(
              token -> {
                refreshTokenRepository
                    .findByToken(token)
                    .ifPresent(
                        rt -> {
                          refreshTokenRepository.delete(rt);
                          log.info("[AuthService] 리프레시 토큰 삭제 완료 - memberId: {}", rt.getMemberId());
                        });
              });
    }

    clearCookie(response, ACCESS_TOKEN_COOKIE);
    clearCookie(response, REFRESH_TOKEN_COOKIE);
    log.info("[AuthService] 로그아웃 완료 - 쿠키 삭제");
  }

  @Transactional
  public KakaoLoginResult kakaoLogin(String code, HttpServletResponse response) {
    log.info("[AuthService] 카카오 로그인 요청");

    KakaoUserInfoResponse userInfo =
        kakaoAuthService.getUserInfo(kakaoAuthService.exchangeCodeForToken(code).getAccessToken());

    String socialId = userInfo.socialId();
    User user =
        userRepository
            .findBySocialId(socialId)
            .orElseGet(
                () -> {
                  log.info("[AuthService] 카카오 신규 회원 가입 - socialId: {}", socialId);
                  return userRepository.save(
                      User.ofKakao(
                          socialId, userInfo.nickname(), userInfo.email(), userInfo.name()));
                });

    CustomUserDetails userDetails = new CustomUserDetails(user);
    String accessToken =
        jwtProvider.generateAccessToken(user.getEmail(), userDetails.getAuthorities());
    String refreshToken = jwtProvider.generateRefreshToken(user.getEmail());

    refreshTokenRepository
        .findByMemberId(user.getId())
        .ifPresentOrElse(
            rt -> {
              rt.rotate(refreshToken, jwtProperties.getRefreshExpiration());
              log.info("[AuthService] 카카오 로그인 - 리프레시 토큰 갱신, userId: {}", user.getId());
            },
            () -> {
              refreshTokenRepository.save(
                  RefreshToken.create(
                      user.getId(), refreshToken, jwtProperties.getRefreshExpiration()));
              log.info("[AuthService] 카카오 로그인 - 리프레시 토큰 신규 발급, userId: {}", user.getId());
            });

    setAccessTokenCookie(response, accessToken);
    setRefreshTokenCookie(response, refreshToken);
    boolean needsName = user.getName() == null || user.getName().isBlank();
    log.info("[AuthService] 카카오 로그인 완료 - userId: {}, needsName: {}", user.getId(), needsName);
    return new KakaoLoginResult(accessToken, refreshToken, needsName);
  }

  private void setAccessTokenCookie(HttpServletResponse response, String token) {
    Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE, token);
    cookie.setHttpOnly(true);
    cookie.setSecure(jwtProperties.isSecure());
    cookie.setPath("/");
    cookie.setMaxAge((int) (jwtProperties.getAccessExpiration() / 1000));
    response.addCookie(cookie);
  }

  private void setRefreshTokenCookie(HttpServletResponse response, String token) {
    Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, token);
    cookie.setHttpOnly(true);
    cookie.setSecure(jwtProperties.isSecure());
    cookie.setPath("/");
    cookie.setMaxAge((int) (jwtProperties.getRefreshExpiration() / 1000));
    response.addCookie(cookie);
  }

  private void clearCookie(HttpServletResponse response, String name) {
    Cookie cookie = new Cookie(name, null);
    cookie.setHttpOnly(true);
    cookie.setSecure(jwtProperties.isSecure());
    cookie.setPath("/");
    cookie.setMaxAge(0);
    response.addCookie(cookie);
  }

  private String extractRefreshTokenCookie(HttpServletRequest request) {
    if (request.getCookies() == null) {
      log.warn("[AuthService] 토큰 재발급 실패 - 쿠키 없음");
      throw new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }
    return Arrays.stream(request.getCookies())
        .filter(c -> REFRESH_TOKEN_COOKIE.equals(c.getName()))
        .map(Cookie::getValue)
        .findFirst()
        .orElseThrow(
            () -> {
              log.warn("[AuthService] 토큰 재발급 실패 - 리프레시 토큰 쿠키 없음");
              return new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
            });
  }
}
