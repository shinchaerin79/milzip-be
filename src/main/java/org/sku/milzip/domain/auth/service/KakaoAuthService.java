package org.sku.milzip.domain.auth.service;

import org.sku.milzip.domain.auth.dto.KakaoTokenResponse;
import org.sku.milzip.domain.auth.dto.KakaoUserInfoResponse;
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
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("grant_type", "authorization_code");
    params.add("client_id", kakaoProperties.getClientId());
    params.add("redirect_uri", kakaoProperties.getRedirectUri());
    params.add("code", code);

    try {
      return restClient
          .post()
          .uri(kakaoProperties.getTokenUrl())
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(params)
          .retrieve()
          .body(KakaoTokenResponse.class);
    } catch (RestClientException e) {
      log.error("[KakaoAuthService] 카카오 토큰 교환 실패 - code: {}", code, e);
      throw new CustomException(AuthErrorCode.KAKAO_LOGIN_FAILED);
    }
  }

  public KakaoUserInfoResponse getUserInfo(String kakaoAccessToken) {
    try {
      return restClient
          .get()
          .uri(kakaoProperties.getUserInfoUrl())
          .header("Authorization", "Bearer " + kakaoAccessToken)
          .retrieve()
          .body(KakaoUserInfoResponse.class);
    } catch (RestClientException e) {
      log.error("[KakaoAuthService] 카카오 사용자 정보 조회 실패", e);
      throw new CustomException(AuthErrorCode.KAKAO_LOGIN_FAILED);
    }
  }
}
