package org.sku.milzip.global.s3.service;

import org.sku.milzip.global.s3.enums.PathName;
import org.springframework.web.multipart.MultipartFile;

public interface S3Service {

  /**
   * 이미지 파일을 S3에 업로드하고 URL을 반환합니다.
   *
   * @param pathName 저장할 경로 (STORE, BENEFIT)
   * @param file 업로드할 이미지 파일
   * @return 업로드된 이미지의 S3 URL
   */
  String uploadFile(PathName pathName, MultipartFile file);

  /**
   * keyName으로 S3 이미지를 삭제합니다.
   *
   * @param keyName S3 객체 키
   */
  void deleteFile(String keyName);

  /**
   * 이미지 URL에서 keyName을 추출합니다.
   *
   * @param imageUrl S3 이미지 URL
   * @return keyName 문자열
   */
  String extractKeyNameFromUrl(String imageUrl);
}
