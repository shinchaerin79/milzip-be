package org.sku.milzip.domain.auth.service;

import org.sku.milzip.domain.auth.dto.response.KakaoTokenResponse;
import org.sku.milzip.domain.auth.dto.response.KakaoUserInfoResponse;
import org.sku.milzip.domain.auth.exception.AuthErrorCode;
import org.sku.milzip.global.config.properties.KakaoProperties;
import org.sku.milzip.global.exception.CustomException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoAuthService {

  private final KakaoProperties kakaoProperties;
  private final RestClient restClient;

  public String buildFrontendRedirectUrl() {
    return kakaoProperties.getFrontendRedirectUrl();
  }

  public String buildAuthorizationUrl() {
    return kakaoProperties.getAuthUrl()
        + "?client_id="
        + kakaoProperties.getClientId()
        + "&redirect_uri="
        + kakaoProperties.getRedirectUri()
        + "&response_type=code";
  }

  public KakaoTokenResponse exchangeCodeForToken(String code) {
    log.info("[KakaoAuthService] 카카오 인가코드 → 액세스 토큰 교환 요청");
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("grant_type", "authorization_code");
    params.add("client_id", kakaoProperties.getClientId());
    params.add("redirect_uri", kakaoProperties.getRedirectUri());
    params.add("code", code);

    try {
      KakaoTokenResponse response =
          restClient
              .post()
              .uri(kakaoProperties.getTokenUrl())
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(params)
              .retrieve()
              .body(KakaoTokenResponse.class);
      log.info("[KakaoAuthService] 카카오 액세스 토큰 교환 완료");
      return response;
    } catch (RestClientException e) {
      log.error("[KakaoAuthService] 카카오 토큰 교환 실패 - code: {}, error: {}", code, e.getMessage(), e);
      throw new CustomException(AuthErrorCode.KAKAO_LOGIN_FAILED);
    }
  }

  public KakaoUserInfoResponse getUserInfo(String kakaoAccessToken) {
    log.debug("[KakaoAuthService] 카카오 사용자 정보 조회 요청");
    try {
      KakaoUserInfoResponse response =
          restClient
              .get()
              .uri(kakaoProperties.getUserInfoUrl())
              .header("Authorization", "Bearer " + kakaoAccessToken)
              .retrieve()
              .body(KakaoUserInfoResponse.class);
      log.info("[KakaoAuthService] 카카오 사용자 정보 조회 완료 - socialId: {}", response.socialId());
      return response;
    } catch (RestClientException e) {
      log.error("[KakaoAuthService] 카카오 사용자 정보 조회 실패 - error: {}", e.getMessage(), e);
      throw new CustomException(AuthErrorCode.KAKAO_LOGIN_FAILED);
    }
  }
}
