package org.sku.milzip.global.codef;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import org.sku.milzip.domain.military.exception.MilitaryErrorCode;
import org.sku.milzip.global.exception.CustomException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class CodefTokenManager {

  private static final String OAUTH_TOKEN_URL = "https://oauth.codef.io/oauth/token";
  private static final long EXPIRY_BUFFER_SECONDS = 60;

  private final CodefProperties codefProperties;
  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  private volatile String cachedToken = null;

  public String getToken() {
    if (isExpired(cachedToken)) {
      refreshToken();
    }
    return cachedToken;
  }

  private boolean isExpired(String token) {
    if (token == null) return true;
    try {
      String[] parts = token.split("\\.");
      if (parts.length < 2) return true;
      byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
      String payload = new String(payloadBytes, StandardCharsets.UTF_8);
      JsonNode node = objectMapper.readTree(payload);
      long exp = node.get("exp").asLong();
      return Instant.now().getEpochSecond() >= exp - EXPIRY_BUFFER_SECONDS;
    } catch (Exception e) {
      log.warn("[CodefTokenManager] 토큰 만료 시각 파싱 실패, 갱신 진행", e);
      return true;
    }
  }

  private synchronized void refreshToken() {
    if (!isExpired(cachedToken)) {
      return;
    }
    log.info("[CodefTokenManager] Codef 액세스 토큰 갱신 시작");
    try {
      String credentials =
          Base64.getEncoder()
              .encodeToString(
                  (codefProperties.getClientId() + ":" + codefProperties.getClientSecret())
                      .getBytes(StandardCharsets.UTF_8));

      String response =
          restClient
              .post()
              .uri(OAUTH_TOKEN_URL)
              .header("Authorization", "Basic " + credentials)
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body("grant_type=client_credentials&scope=read")
              .retrieve()
              .body(String.class);

      JsonNode node = objectMapper.readTree(response);
      this.cachedToken = node.get("access_token").asText();
      log.info("[CodefTokenManager] Codef 액세스 토큰 갱신 완료");
    } catch (Exception e) {
      log.error("[CodefTokenManager] Codef 액세스 토큰 갱신 실패", e);
      throw new CustomException(MilitaryErrorCode.CODEF_API_FAILED);
    }
  }
}
