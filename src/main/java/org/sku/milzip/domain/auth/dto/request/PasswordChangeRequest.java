package org.sku.milzip.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "비밀번호 변경 요청")
public record PasswordChangeRequest(
    @Schema(description = "이메일", example = "user@example.com") @NotBlank @Email String email,
    @Schema(description = "새 비밀번호 (8~20자, 영문·숫자·특수문자 각 1자 이상 포함)", example = "NewPass1!")
        @NotBlank
        @Size(min = 8, max = 20)
        @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,20}$",
            message = "비밀번호는 영문, 숫자, 특수문자(!@#$%^&*)를 각 1자 이상 포함해야 합니다.")
        String newPassword) {}
