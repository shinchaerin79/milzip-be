package org.sku.milzip.domain.auth.exception;

import org.sku.milzip.global.exception.model.BaseErrorCode;
import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

  // 400 Bad Request
  INVALID_CREDENTIALS("AUTH4001", "아이디 또는 비밀번호가 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
  DUPLICATE_EMAIL("AUTH4002", "이미 사용 중인 이메일입니다.", HttpStatus.BAD_REQUEST),
  EMAIL_NOT_VERIFIED("AUTH4003", "이메일 인증이 완료되지 않았습니다.", HttpStatus.BAD_REQUEST),
  VERIFICATION_CODE_INVALID("AUTH4004", "인증 코드가 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
  VERIFICATION_CODE_EXPIRED("AUTH4005", "인증 코드가 만료되었습니다.", HttpStatus.BAD_REQUEST),

  // 401 Unauthorized
  AUTHENTICATION_REQUIRED("AUTH4011", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
  UNAUTHORIZED_TOKEN("AUTH4012", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
  EXPIRED_ACCESS_TOKEN("AUTH4013", "만료된 액세스 토큰입니다. 리프레시가 필요합니다.", HttpStatus.UNAUTHORIZED),
  EXPIRED_REFRESH_TOKEN("AUTH4014", "만료된 리프레시 토큰입니다. 재로그인이 필요합니다.", HttpStatus.UNAUTHORIZED),
  INVALID_REFRESH_TOKEN("AUTH4015", "유효하지 않은 리프레시 토큰입니다.", HttpStatus.UNAUTHORIZED),

  // 404 Not Found
  USER_NOT_FOUND("AUTH4041", "존재하지 않는 회원입니다.", HttpStatus.NOT_FOUND),

  // 500 Internal Server Error
  EMAIL_SEND_FAILED("AUTH5001", "이메일 전송에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  EMAIL_AUTH_FAILED(
      "AUTH5002", "메일 서버 인증에 실패했습니다. 서버 관리자에게 문의해 주세요.", HttpStatus.INTERNAL_SERVER_ERROR);

  private final String code;
  private final String message;
  private final HttpStatus status;
}
