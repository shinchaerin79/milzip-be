package org.sku.milzip.global.s3.controller;

import org.sku.milzip.global.common.BaseResponse;
import org.sku.milzip.global.s3.enums.PathName;
import org.sku.milzip.global.s3.service.S3Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "S3", description = "파일 업로드 API")
@RestController
@RequestMapping("/s3")
@ConditionalOnBean(S3Service.class)
@RequiredArgsConstructor
public class S3Controller {

  private final S3Service s3Service;

  @Operation(summary = "[ 전체 | 토큰 X | 파일 업로드 ]")
  @PostMapping(value = "/upload", consumes = "multipart/form-data")
  public BaseResponse<String> upload(
      @Parameter(description = "저장 경로", example = "STORE") @RequestParam PathName pathName,
      @RequestParam("file") MultipartFile file) {
    return BaseResponse.success(s3Service.uploadFile(pathName, file));
  }

  @Operation(summary = "[ 전체 | 토큰 X | 파일 삭제 ]")
  @DeleteMapping("/delete")
  public BaseResponse<Void> delete(
      @Parameter(description = "S3 keyName (URL 아님)", example = "store/uuid.jpg") @RequestParam
          String keyName) {
    s3Service.deleteFile(keyName);
    return BaseResponse.success(null);
  }
}
