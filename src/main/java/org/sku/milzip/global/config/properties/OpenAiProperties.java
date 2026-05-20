package org.sku.milzip.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@ConfigurationProperties("openai")
public class OpenAiProperties {

  private String apiKey;
  private String embeddingModel;
  private String chatModel;
}
