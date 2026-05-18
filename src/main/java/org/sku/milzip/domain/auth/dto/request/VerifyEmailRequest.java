package org.sku.milzip.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이메일 인증 코드 확인 요청")
public record VerifyEmailRequest(
    @Schema(description = "이메일", example = "user@example.com") @NotBlank @Email String email,
    @Schema(description = "인증 코드 (6자리)", example = "123456") @NotBlank String code) {}
