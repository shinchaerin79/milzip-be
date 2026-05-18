package org.sku.milzip.domain.military.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MilitaryVerificationRequest(
    @NotBlank @Pattern(regexp = "\\d{13}", message = "주민등록번호는 13자리 숫자여야 합니다.") String identity,
    @NotBlank @Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 전화번호 형식이 아닙니다.") String phoneNo,
    @NotBlank String addrSido,
    @NotBlank String addrSigungu) {}
