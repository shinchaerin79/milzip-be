package org.sku.milzip.domain.benefit.exception;

import org.sku.milzip.global.exception.model.BaseErrorCode;
import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BenefitErrorCode implements BaseErrorCode {
  BENEFIT_NOT_FOUND("BEN4041", "존재하지 않는 혜택입니다.", HttpStatus.NOT_FOUND),
  TMO_NOT_FOUND("BEN4042", "존재하지 않는 TMO입니다.", HttpStatus.NOT_FOUND);

  private final String code;
  private final String message;
  private final HttpStatus status;
}
