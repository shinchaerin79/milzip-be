package org.sku.milzip.domain.user.exception;

import org.sku.milzip.global.exception.model.BaseErrorCode;
import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements BaseErrorCode {

  // 404 Not Found
  FAVORITE_NOT_FOUND("USR4041", "즐겨찾기 내역이 존재하지 않습니다.", HttpStatus.NOT_FOUND),
  BENEFIT_FAVORITE_NOT_FOUND("USR4042", "혜택 즐겨찾기 내역이 존재하지 않습니다.", HttpStatus.NOT_FOUND),
  TMO_FAVORITE_NOT_FOUND("USR4043", "TMO 즐겨찾기 내역이 존재하지 않습니다.", HttpStatus.NOT_FOUND),

  // 409 Conflict
  FAVORITE_ALREADY_EXISTS("USR4091", "이미 즐겨찾기에 추가된 매장입니다.", HttpStatus.CONFLICT),
  BENEFIT_FAVORITE_ALREADY_EXISTS("USR4094", "이미 즐겨찾기에 추가된 혜택입니다.", HttpStatus.CONFLICT),
  TMO_FAVORITE_ALREADY_EXISTS("USR4095", "이미 즐겨찾기에 추가된 TMO입니다.", HttpStatus.CONFLICT),
  EMAIL_ALREADY_EXISTS("USR4092", "이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT),
  NICKNAME_ALREADY_EXISTS("USR4093", "이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT);

  private final String code;
  private final String message;
  private final HttpStatus status;
}
