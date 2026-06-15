package org.sku.milzip.domain.location.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "지오코딩 결과")
public record GeocodeResponse(
    @Schema(description = "주소", example = "강릉시 강문동") String address,
    @Schema(description = "위도", example = "37.803") double lat,
    @Schema(description = "경도", example = "128.909") double lng) {}
