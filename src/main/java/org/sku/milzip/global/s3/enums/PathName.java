package org.sku.milzip.global.s3.enums;

import io.swagger.v3.oas.annotations.media.Schema;

public enum PathName {
  @Schema(description = "가게 이미지")
  STORE,

  @Schema(description = "혜택 이미지")
  BENEFIT,

  @Schema(description = "리뷰 이미지")
  REVIEW,

  @Schema(description = "사용자 프로필 이미지")
  PROFILE;
}
