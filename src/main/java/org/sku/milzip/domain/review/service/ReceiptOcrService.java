package org.sku.milzip.domain.review.service;

import org.sku.milzip.domain.review.dto.response.ReceiptVerifyResponse;
import org.sku.milzip.domain.store.entity.Store;
import org.sku.milzip.domain.store.exception.StoreErrorCode;
import org.sku.milzip.domain.store.repository.StoreRepository;
import org.sku.milzip.global.exception.CustomException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptOcrService {

  private final StoreRepository storeRepository;

  /**
   * 영수증 이미지를 OCR로 분석하여 해당 매장과 일치하는지 검증합니다.
   *
   * <p>TODO: Naver Clova OCR API 연동 필요 - API:
   * https://api.ncloud-docs.com/docs/ai-application-service-ocr-general - application.yml에
   * naver.ocr.secret, naver.ocr.invoke-url 설정 추가
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

    // TODO: Naver Clova OCR API 호출하여 영수증 텍스트 추출
    // String ocrText = callNaverClovaOcr(receiptImage);
    // String recognizedStoreName = extractStoreName(ocrText);
    // boolean verified = isStoreNameMatch(store.getName(), recognizedStoreName);

    // 임시: OCR 미연동 상태에서는 항상 검증 통과 처리
    String recognizedStoreName = store.getName();
    boolean verified = true;

    return ReceiptVerifyResponse.builder()
        .verified(verified)
        .recognizedStoreName(recognizedStoreName)
        .message(verified ? "영수증의 가게명이 일치합니다." : "영수증의 가게명이 일치하지 않습니다.")
        .build();
  }
}
