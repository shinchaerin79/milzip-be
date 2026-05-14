package org.sku.milzip.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendVerificationEmailRequest(@NotBlank @Email String email) {}
