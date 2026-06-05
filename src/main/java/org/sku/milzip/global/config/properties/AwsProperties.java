package org.sku.milzip.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@ConfigurationProperties("cloud.aws")
public class AwsProperties {

  private String region;
  private Credentials credentials;
  private S3 s3;

  @Getter
  @AllArgsConstructor
  public static class Credentials {
    private String accessKey;
    private String secretKey;
  }

  @Getter
  @AllArgsConstructor
  public static class S3 {
    private String bucket;
    private Path path;

    @Getter
    @AllArgsConstructor
    public static class Path {
      private String store;
      private String benefit;
      private String review;
      private String profile;
    }
  }
}
