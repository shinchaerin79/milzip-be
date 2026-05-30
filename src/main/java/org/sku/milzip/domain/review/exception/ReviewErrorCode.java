package org.sku.milzip.domain.review.exception;

import org.sku.milzip.global.exception.model.BaseErrorCode;
import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements BaseErrorCode {

  // 404 Not Found
  REVIEW_NOT_FOUND("REV4041", "존재하지 않는 리뷰입니다.", HttpStatus.NOT_FOUND),

  // 403 Forbidden
  REVIEW_FORBIDDEN("REV4031", "리뷰 수정/삭제 권한이 없습니다.", HttpStatus.FORBIDDEN),

  // 409 Conflict
  REVIEW_ALREADY_EXISTS("REV4091", "이미 해당 영수증으로 리뷰를 작성하셨습니다.", HttpStatus.CONFLICT),

  // 400 Bad Request
  BENEFIT_STATUS_REQUIRED("REV4001", "군인 인증 사용자는 혜택 여부를 선택해야 합니다.", HttpStatus.BAD_REQUEST),

  // 422 Unprocessable Entity
  OCR_STORE_NAME_MISMATCH("REV4221", "영수증의 가게명이 일치하지 않습니다.", HttpStatus.UNPROCESSABLE_ENTITY),

  // 500 Internal Server Error
  OCR_API_ERROR("REV5001", "영수증 OCR 처리 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

  private final String code;
  private final String message;
  private final HttpStatus status;
}
