package org.sku.milzip.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@ConfigurationProperties("kakao")
public class KakaoProperties {

  private String clientId;
  private String localApiKey;
  private String redirectUri;
  private String authUrl;
  private String tokenUrl;
  private String userInfoUrl;
  private String frontendRedirectUrl;
  private String webRedirectUrl;
}
