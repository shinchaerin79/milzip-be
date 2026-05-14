package org.sku.milzip.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@ConfigurationProperties("cors")
public class CorsProperties {

  private String[] allowedOrigins;
}
