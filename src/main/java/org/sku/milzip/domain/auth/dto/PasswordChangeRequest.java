package org.sku.milzip.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
    @NotBlank @Email String email,
    @NotBlank
        @Size(min = 8, max = 20)
        @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,20}$",
            message = "비밀번호는 영문, 숫자, 특수문자(!@#$%^&*)를 각 1자 이상 포함해야 합니다.")
        String newPassword) {}
