package org.sku.milzip.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@ConfigurationProperties("jwt")
public class JwtProperties {

  private String secret;
  private long accessExpiration;
  private long refreshExpiration;
  private boolean secure;
  private String sameSite;
  private String cookieDomain;
}
