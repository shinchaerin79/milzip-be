package org.sku.milzip.domain.military.exception;

import org.sku.milzip.global.exception.model.BaseErrorCode;
import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MilitaryErrorCode implements BaseErrorCode {

  // 400 Bad Request
  ALREADY_VERIFIED("MIL4001", "이미 군인 인증이 완료된 사용자입니다.", HttpStatus.BAD_REQUEST),
  USER_NAME_MISSING("MIL4002", "이름 정보가 없습니다. 회원정보를 확인해 주세요.", HttpStatus.BAD_REQUEST),

  // 404 Not Found
  VERIFICATION_NOT_FOUND("MIL4041", "진행 중인 인증 요청이 없습니다.", HttpStatus.NOT_FOUND),

  // 408 Request Timeout
  VERIFICATION_EXPIRED("MIL4081", "인증 시간이 초과되었습니다. 다시 시도해 주세요.", HttpStatus.REQUEST_TIMEOUT),

  // 400 Bad Request (사용자 액션 실패)
  KAKAO_AUTH_CANCELLED(
      "MIL4003", "카카오 간편인증이 취소되었거나 시간이 초과되었습니다. 다시 시도해 주세요.", HttpStatus.BAD_REQUEST),

  // 422 Unprocessable Entity
  NOT_INTERNET_ISSUABLE(
      "MIL4221", "인터넷 발급 비대상입니다. 가까운 주민센터에서 팩스민원으로 신청 바랍니다.", HttpStatus.UNPROCESSABLE_ENTITY),

  // 500 Internal Server Error
  CODEF_API_FAILED("MIL5001", "병적증명서 조회 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

  private final String code;
  private final String message;
  private final HttpStatus status;
}
