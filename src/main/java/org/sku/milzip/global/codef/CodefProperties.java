package org.sku.milzip.global.codef;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@ConfigurationProperties("codef")
public class CodefProperties {

  private String baseUrl;
  private String clientId;
  private String clientSecret;
  private String publicKey;

  public String getMilitaryUrl() {
    return baseUrl + "/v1/kr/public/mw/certificate/military";
  }
}
