package org.sku.milzip.domain.military.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "군인 인증 요청")
public record MilitaryVerificationRequest(
    @Schema(description = "주민등록번호 13자리 (숫자만)", example = "0307094080912")
        @NotBlank
        @Pattern(regexp = "\\d{13}", message = "주민등록번호는 13자리 숫자여야 합니다.")
        String identity,
    @Schema(description = "카카오톡에 등록된 전화번호", example = "01012345678")
        @NotBlank
        @Pattern(regexp = "^01[0-9]{8,9}$", message = "올바른 전화번호 형식이 아닙니다.")
        String phoneNo,
    @Schema(description = "주민등록상 시도", example = "서울특별시") @NotBlank String addrSido,
    @Schema(description = "주민등록상 시군구", example = "강남구") @NotBlank String addrSigungu) {}
