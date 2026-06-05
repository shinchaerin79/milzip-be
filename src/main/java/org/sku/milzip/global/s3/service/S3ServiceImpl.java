package org.sku.milzip.global.s3.service;

import java.util.UUID;

import org.sku.milzip.global.config.properties.AwsProperties;
import org.sku.milzip.global.exception.CustomException;
import org.sku.milzip.global.s3.enums.PathName;
import org.sku.milzip.global.s3.exception.S3ErrorCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
@ConditionalOnBean(S3Client.class)
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {

  private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

  private final S3Client s3Client;
  private final AwsProperties awsProperties;

  @Override
  public String uploadFile(PathName pathName, MultipartFile file) {
    validateFile(file);

    String extension = extractExtension(file);
    String keyName = getPrefix(pathName) + "/" + UUID.randomUUID() + "." + extension;

    try {
      s3Client.putObject(
          PutObjectRequest.builder()
              .bucket(awsProperties.getS3().getBucket())
              .key(keyName)
              .contentType(file.getContentType())
              .contentLength(file.getSize())
              .build(),
          RequestBody.fromBytes(file.getBytes()));

      log.info("[S3] 업로드 완료 - keyName: {}", keyName);
      return buildUrl(keyName);

    } catch (CustomException e) {
      throw e;
    } catch (Exception e) {
      log.error("[S3] 업로드 실패 - keyName: {}", keyName, e);
      throw new CustomException(S3ErrorCode.FILE_SERVER_ERROR);
    }
  }

  @Override
  public void deleteFile(String keyName) {
    assertFileExists(keyName);

    try {
      s3Client.deleteObject(
          DeleteObjectRequest.builder()
              .bucket(awsProperties.getS3().getBucket())
              .key(keyName)
              .build());

      log.info("[S3] 삭제 완료 - keyName: {}", keyName);

    } catch (Exception e) {
      log.error("[S3] 삭제 실패 - keyName: {}", keyName, e);
      throw new CustomException(S3ErrorCode.FILE_SERVER_ERROR);
    }
  }

  @Override
  public String extractKeyNameFromUrl(String imageUrl) {
    String bucketUrl = getBucketUrl();
    if (imageUrl == null || !imageUrl.startsWith(bucketUrl)) {
      log.error("[S3] 유효하지 않은 URL - imageUrl: {}", imageUrl);
      throw new CustomException(S3ErrorCode.FILE_URL_INVALID);
    }
    return imageUrl.substring(bucketUrl.length());
  }

  private void assertFileExists(String keyName) {
    try {
      s3Client.headObject(
          HeadObjectRequest.builder()
              .bucket(awsProperties.getS3().getBucket())
              .key(keyName)
              .build());
    } catch (NoSuchKeyException e) {
      log.error("[S3] 존재하지 않는 파일 - keyName: {}", keyName);
      throw new CustomException(S3ErrorCode.FILE_NOT_FOUND);
    } catch (Exception e) {
      log.error("[S3] 파일 존재 확인 실패 - keyName: {}", keyName, e);
      throw new CustomException(S3ErrorCode.FILE_SERVER_ERROR);
    }
  }

  private void validateFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new CustomException(S3ErrorCode.FILE_NOT_FOUND);
    }
    if (file.getSize() > MAX_FILE_SIZE) {
      throw new CustomException(S3ErrorCode.FILE_SIZE_INVALID);
    }
    String contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      throw new CustomException(S3ErrorCode.FILE_TYPE_INVALID);
    }
  }

  private String extractExtension(MultipartFile file) {
    String name = file.getOriginalFilename();
    if (name == null || !name.contains(".")) return "jpg";
    return name.substring(name.lastIndexOf('.') + 1).toLowerCase();
  }

  private String buildUrl(String keyName) {
    return getBucketUrl() + keyName;
  }

  private String getBucketUrl() {
    return "https://"
        + awsProperties.getS3().getBucket()
        + ".s3."
        + awsProperties.getRegion()
        + ".amazonaws.com/";
  }

  private String getPrefix(PathName pathName) {
    return switch (pathName) {
      case STORE -> awsProperties.getS3().getPath().getStore();
      case BENEFIT -> awsProperties.getS3().getPath().getBenefit();
      case REVIEW -> awsProperties.getS3().getPath().getReview();
      case PROFILE -> awsProperties.getS3().getPath().getProfile();
    };
  }
}
