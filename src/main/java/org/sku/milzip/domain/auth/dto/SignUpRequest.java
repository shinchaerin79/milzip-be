package org.sku.milzip.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

public record SignUpRequest(
    @Schema(description = "이메일", example = "user@example.com") @NotBlank @Email String email,
    @Schema(description = "비밀번호 (8~20자, 영문·숫자·특수문자 각 1자 이상 포함)", example = "Password1!")
        @NotBlank
        @Size(min = 8, max = 20)
        @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*]).+$",
            message = "비밀번호는 영문, 숫자, 특수문자(!@#$%^&*)를 각 1자 이상 포함해야 합니다.")
        String password,
    @Schema(description = "닉네임 (2~20자)", example = "밀집이") @NotBlank @Size(min = 2, max = 20)
        String nickname,
    @Schema(description = "본명 (실명인증에 사용)", example = "홍길동") @NotBlank String name) {}
