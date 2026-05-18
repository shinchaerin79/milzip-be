package org.sku.milzip.domain.store.exception;

import org.sku.milzip.global.exception.model.BaseErrorCode;
import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StoreErrorCode implements BaseErrorCode {

  // 404 Not Found
  STORE_NOT_FOUND("STO4041", "존재하지 않는 매장입니다.", HttpStatus.NOT_FOUND);

  private final String code;
  private final String message;
  private final HttpStatus status;
}
