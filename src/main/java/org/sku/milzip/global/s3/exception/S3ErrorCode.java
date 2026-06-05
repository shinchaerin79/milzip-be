package org.sku.milzip.global.s3.exception;

import org.sku.milzip.global.exception.model.BaseErrorCode;
import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum S3ErrorCode implements BaseErrorCode {
  FILE_SIZE_INVALID("IMG4001", "파일 크기는 10MB를 초과할 수 없습니다.", HttpStatus.BAD_REQUEST),
  FILE_TYPE_INVALID("IMG4002", "이미지 파일만 업로드 가능합니다.", HttpStatus.BAD_REQUEST),
  FILE_URL_INVALID("IMG4003", "유효하지 않은 이미지 URL입니다.", HttpStatus.BAD_REQUEST),

  FILE_NOT_FOUND("IMG4041", "존재하지 않는 이미지입니다.", HttpStatus.NOT_FOUND),

  FILE_SERVER_ERROR("IMG5001", "이미지 처리 중 서버 에러가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

  private final String code;
  private final String message;
  private final HttpStatus status;
}
