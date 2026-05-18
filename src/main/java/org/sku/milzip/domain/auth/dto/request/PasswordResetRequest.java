package org.sku.milzip.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "비밀번호 재설정 인증 코드 발송 요청")
public record PasswordResetRequest(
    @Schema(description = "이메일", example = "user@example.com") @NotBlank @Email String email) {}
