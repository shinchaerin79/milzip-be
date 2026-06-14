package org.sku.milzip.domain.auth.dto.response;

public record KakaoLoginResult(String accessToken, String refreshToken, boolean needsName) {}
