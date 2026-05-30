package org.sku.milzip.domain.review.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import org.sku.milzip.domain.review.dto.response.ReceiptVerifyResponse;
import org.sku.milzip.domain.review.exception.ReviewErrorCode;
import org.sku.milzip.domain.store.entity.Store;
import org.sku.milzip.domain.store.exception.StoreErrorCode;
import org.sku.milzip.domain.store.repository.StoreRepository;
import org.sku.milzip.global.exception.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptOcrService {

  private final StoreRepository storeRepository;
  private final ObjectMapper objectMapper;

  @Value("${naver.ocr.secret}")
  private String ocrSecret;

  @Value("${naver.ocr.invoke-url}")
  private String ocrInvokeUrl;

  /**
   * 영수증 이미지를 OCR로 분석하여 해당 매장과 일치하는지 검증합니다.
   *
   * <p>API: https://api.ncloud-docs.com/docs/ai-application-service-ocr-general
   */
  public ReceiptVerifyResponse verifyReceipt(Long storeId, MultipartFile receiptImage) {
    Store store =
        storeRepository
            .findById(storeId)
            .orElseThrow(() -> new CustomException(StoreErrorCode.STORE_NOT_FOUND));

    log.info(
        "[OCR] 영수증 검증 요청 - storeId: {}, storeName: {}, fileSize: {}bytes",
        storeId,
        store.getName(),
        receiptImage.getSize());

    String ocrText = callNaverClovaOcr(receiptImage);
    log.info("[OCR] 전체 추출 텍스트:\n{}", ocrText);

    String recognizedStoreName = extractStoreName(ocrText);
    boolean verified = isStoreNameMatch(store.getName(), recognizedStoreName);

    log.info(
        "[OCR] 검증 결과 - 매장명: [{}], OCR 인식: [{}], 일치: {}",
        store.getName(),
        recognizedStoreName,
        verified);

    return ReceiptVerifyResponse.builder()
        .verified(verified)
        .recognizedStoreName(recognizedStoreName)
        .message(verified ? "영수증의 가게명이 일치합니다." : "영수증의 가게명이 일치하지 않습니다.")
        .build();
  }

  // Private helpers

  /**
   * Naver Clova OCR API를 호출하여 영수증 전체 텍스트를 반환합니다.
   *
   * <p>이미지를 Base64로 인코딩하여 JSON body로 전송합니다. (Content-Type: application/json) HEIC 파일은 JPEG로 변환 후
   * 전송합니다.
   */
  private String callNaverClovaOcr(MultipartFile imageFile) {
    try {
      String requestId = UUID.randomUUID().toString();

      byte[] imageBytes;
      String imageFormat;
      if (isHeic(imageFile)) {
        log.info("[OCR] HEIC 감지 → JPEG 변환 중");
        imageBytes = convertHeicToJpeg(imageFile.getBytes());
        imageFormat = "jpeg";
      } else {
        imageBytes = imageFile.getBytes();
        imageFormat = resolveImageFormat(imageFile.getOriginalFilename());
      }

      String base64Image = Base64.getEncoder().encodeToString(imageBytes);

      String requestBody =
          String.format(
              "{\"version\":\"V2\",\"requestId\":\"%s\",\"timestamp\":%d,"
                  + "\"images\":[{\"format\":\"%s\",\"name\":\"receipt\",\"data\":\"%s\"}]}",
              requestId, System.currentTimeMillis(), imageFormat, base64Image);

      String responseBody =
          RestClient.create()
              .post()
              .uri(ocrInvokeUrl)
              .header("X-OCR-SECRET", ocrSecret)
              .contentType(MediaType.APPLICATION_JSON)
              .body(requestBody)
              .retrieve()
              .body(String.class);

      return parseOcrText(responseBody);

    } catch (IOException e) {
      log.error("[OCR] 이미지 파일 읽기 실패: {}", e.getMessage());
      throw new CustomException(ReviewErrorCode.OCR_API_ERROR);
    } catch (Exception e) {
      log.error("[OCR] API 호출 실패: {}", e.getMessage());
      throw new CustomException(ReviewErrorCode.OCR_API_ERROR);
    }
  }

  /** OCR 응답 JSON에서 텍스트를 줄 단위로 조합합니다. */
  private String parseOcrText(String responseBody) {
    try {
      JsonNode root = objectMapper.readTree(responseBody);
      JsonNode image = root.path("images").get(0);

      if (!"SUCCESS".equals(image.path("inferResult").asText())) {
        log.warn("[OCR] 이미지 인식 실패: {}", image.path("message").asText());
        throw new CustomException(ReviewErrorCode.OCR_API_ERROR);
      }

      JsonNode fields = image.path("fields");
      StringBuilder sb = new StringBuilder();
      for (JsonNode field : fields) {
        sb.append(field.path("inferText").asText());
        if (field.path("lineBreak").asBoolean()) {
          sb.append("\n");
        } else {
          sb.append(" ");
        }
      }
      return sb.toString().trim();

    } catch (CustomException e) {
      throw e;
    } catch (Exception e) {
      log.error("[OCR] 응답 파싱 실패: {}", e.getMessage());
      throw new CustomException(ReviewErrorCode.OCR_API_ERROR);
    }
  }

  /**
   * 영수증 상단에서 가게명을 추출합니다.
   *
   * <p>영수증은 보통 상단 1~5줄에 가게명이 위치하므로, 의미 있는 첫 번째 줄을 반환합니다.
   */
  private String extractStoreName(String ocrText) {
    String[] lines = ocrText.split("\n");
    for (String line : lines) {
      String trimmed = line.trim();
      // 2글자 이상이고 숫자로만 구성되지 않은 줄 → 가게명으로 판단
      if (trimmed.length() >= 2 && !trimmed.matches("[0-9\\-\\s]+")) {
        return trimmed;
      }
    }
    return lines.length > 0 ? lines[0].trim() : "";
  }

  /**
   * 매장명과 OCR 인식 가게명의 일치 여부를 확인합니다.
   *
   * <p>공백·특수문자를 제거하고 소문자로 정규화한 뒤 포함 관계로 비교합니다.
   */
  private boolean isStoreNameMatch(String storeName, String recognizedName) {
    if (recognizedName == null || recognizedName.isBlank()) {
      return false;
    }
    String n1 = normalize(storeName);
    String n2 = normalize(recognizedName);
    return n1.contains(n2) || n2.contains(n1);
  }

  private String normalize(String text) {
    return text.replaceAll("[\\s·•&()\\.\\-]", "").toLowerCase();
  }

  private String resolveImageFormat(String filename) {
    if (filename == null) return "jpeg";
    String lower = filename.toLowerCase();
    if (lower.endsWith(".png")) return "png";
    if (lower.endsWith(".pdf")) return "pdf";
    if (lower.endsWith(".tiff") || lower.endsWith(".tif")) return "tiff";
    return "jpeg"; // jpg → jpeg (Naver OCR 지원 포맷)
  }

  private static final Set<String> HEIC_CONTENT_TYPES =
      Set.of("image/heic", "image/heif", "image/heic-sequence", "image/heif-sequence");

  private boolean isHeic(MultipartFile file) {
    String contentType = file.getContentType();
    if (contentType != null && HEIC_CONTENT_TYPES.contains(contentType.toLowerCase())) {
      return true;
    }
    String filename = file.getOriginalFilename();
    if (filename != null) {
      String lower = filename.toLowerCase();
      return lower.endsWith(".heic") || lower.endsWith(".heif");
    }
    return false;
  }

  /**
   * HEIC 이미지를 JPEG로 변환합니다.
   *
   * <p>macOS 기본 내장 `sips` 명령어를 사용합니다. Linux 배포 시: ImageMagick(`convert`) 또는
   * `libheif-examples`(`heif-convert`) 설치 필요
   */
  private byte[] convertHeicToJpeg(byte[] heicBytes) throws IOException {
    Path tempHeic = null;
    Path tempJpeg = null;
    try {
      tempHeic = Files.createTempFile("receipt_", ".heic");
      tempJpeg = Files.createTempFile("receipt_", ".jpg");
      Files.write(tempHeic, heicBytes);

      String os = System.getProperty("os.name").toLowerCase();
      ProcessBuilder pb;
      if (os.contains("mac")) {
        // macOS 기본 내장 sips 사용
        pb =
            new ProcessBuilder(
                "sips",
                "-s",
                "format",
                "jpeg",
                tempHeic.toAbsolutePath().toString(),
                "--out",
                tempJpeg.toAbsolutePath().toString());
      } else {
        // Linux: ImageMagick 또는 heif-convert 사용
        pb =
            new ProcessBuilder(
                "convert",
                tempHeic.toAbsolutePath().toString(),
                tempJpeg.toAbsolutePath().toString());
      }

      pb.redirectErrorStream(true);
      Process process = pb.start();
      int exitCode = process.waitFor();

      if (exitCode != 0) {
        throw new IOException("HEIC 변환 실패 (exitCode=" + exitCode + ")");
      }

      byte[] result = Files.readAllBytes(tempJpeg);
      log.info("[OCR] HEIC → JPEG 변환 완료: {}bytes", result.length);
      return result;

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("HEIC 변환 중단됨", e);
    } finally {
      if (tempHeic != null) Files.deleteIfExists(tempHeic);
      if (tempJpeg != null) Files.deleteIfExists(tempJpeg);
    }
  }
}
