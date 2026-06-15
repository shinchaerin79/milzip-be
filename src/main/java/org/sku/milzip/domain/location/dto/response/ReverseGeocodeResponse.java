package org.sku.milzip.domain.location.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "역지오코딩 응답")
public record ReverseGeocodeResponse(
    @Schema(description = "한국어 주소 (구 동)", example = "성북구 돈암동") String address) {}
