package org.sku.milzip.domain.military.service;

import org.sku.milzip.domain.military.dto.request.MilitaryVerificationRequest;
import org.sku.milzip.domain.military.entity.MilitaryVerification;
import org.sku.milzip.domain.military.exception.MilitaryErrorCode;
import org.sku.milzip.domain.military.repository.MilitaryVerificationRepository;
import org.sku.milzip.domain.user.entity.MilitaryStatus;
import org.sku.milzip.domain.user.entity.User;
import org.sku.milzip.domain.user.repository.UserRepository;
import org.sku.milzip.global.codef.CodefService;
import org.sku.milzip.global.exception.CustomException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;

@Slf4j
@Service
@RequiredArgsConstructor
public class MilitaryVerificationService {

  private static final String RESULT_CODE_TWO_WAY = "CF-03002";
  private static final String RESULT_CODE_SUCCESS = "CF-00000";
  private static final String RESULT_CODE_NOT_INTERNET_ISSUABLE = "CF-12100";
  private static final String RESULT_CODE_KAKAO_AUTH_FAILED = "CF-12701";
  private static final String SERVICE_YN_ACTIVE = "복무를 마치지 않은 사람";

  private final MilitaryVerificationRepository militaryVerificationRepository;
  private final UserRepository userRepository;
  private final CodefService codefService;

  @Transactional
  public void startVerification(String email, MilitaryVerificationRequest request) {
    log.info("[MilitaryVerificationService] 군인 인증 시작 요청 - email: {}", email);

    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new CustomException(MilitaryErrorCode.CODEF_API_FAILED));

    if (user.getMilitaryStatus() == MilitaryStatus.VERIFIED) {
      throw new CustomException(MilitaryErrorCode.ALREADY_VERIFIED);
    }

    if (user.getName() == null) {
      log.warn("[MilitaryVerificationService] 이름 정보 없음 - userId: {}", user.getId());
      throw new CustomException(MilitaryErrorCode.USER_NAME_MISSING);
    }

    militaryVerificationRepository.deleteByUserId(user.getId());

    JsonNode response =
        codefService.callMilitary(
            codefService.buildFirstRequest(
                user.getName(),
                request.identity(),
                request.phoneNo(),
                request.addrSido(),
                request.addrSigungu()));

    String resultCode = response.path("result").path("code").asText();
    log.info("[MilitaryVerificationService] CODEF 1차 응답 코드: {}", resultCode);

    if (RESULT_CODE_SUCCESS.equals(resultCode)) {
      processResult(response.path("data"), user);
      log.info("[MilitaryVerificationService] CODEF 직접 응답 처리 완료 - userId: {}", user.getId());
      return;
    }

    if (!RESULT_CODE_TWO_WAY.equals(resultCode)) {
      log.error("[MilitaryVerificationService] CODEF 1차 호출 실패 - code: {}", resultCode);
      throw new CustomException(MilitaryErrorCode.CODEF_API_FAILED);
    }

    JsonNode data = response.path("data");
    MilitaryVerification verification =
        MilitaryVerification.create(
            user.getId(),
            request.identity(),
            request.phoneNo(),
            request.addrSido(),
            request.addrSigungu());
    verification.updateTwoWayInfo(
        data.path("jobIndex").asInt(),
        data.path("threadIndex").asInt(),
        data.path("jti").asText(),
        data.path("twoWayTimestamp").asLong());

    militaryVerificationRepository.save(verification);
    log.info("[MilitaryVerificationService] 카카오톡 인증 요청 전송 완료 - userId: {}", user.getId());
  }

  @Transactional
  public void confirmVerification(String email) {
    log.info("[MilitaryVerificationService] 군인 인증 확인 요청 - email: {}", email);

    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new CustomException(MilitaryErrorCode.CODEF_API_FAILED));

    MilitaryVerification verification =
        militaryVerificationRepository
            .findByUserId(user.getId())
            .orElseThrow(() -> new CustomException(MilitaryErrorCode.VERIFICATION_NOT_FOUND));

    if (verification.isExpired()) {
      militaryVerificationRepository.delete(verification);
      log.warn("[MilitaryVerificationService] 인증 시간 초과 - userId: {}", user.getId());
      throw new CustomException(MilitaryErrorCode.VERIFICATION_EXPIRED);
    }

    JsonNode response =
        codefService.callMilitary(
            codefService.buildSecondRequest(
                user.getName(),
                verification.getIdentity(),
                verification.getPhoneNo(),
                verification.getAddrSido(),
                verification.getAddrSigungu(),
                verification.getJobIndex(),
                verification.getThreadIndex(),
                verification.getJti(),
                verification.getTwoWayTimestamp()));

    String resultCode = response.path("result").path("code").asText();
    log.info("[MilitaryVerificationService] CODEF 2차 응답 코드: {}", resultCode);

    if (RESULT_CODE_NOT_INTERNET_ISSUABLE.equals(resultCode)) {
      militaryVerificationRepository.delete(verification);
      throw new CustomException(MilitaryErrorCode.NOT_INTERNET_ISSUABLE);
    }

    if (RESULT_CODE_KAKAO_AUTH_FAILED.equals(resultCode)) {
      militaryVerificationRepository.delete(verification);
      throw new CustomException(MilitaryErrorCode.KAKAO_AUTH_CANCELLED);
    }

    if (!RESULT_CODE_SUCCESS.equals(resultCode)) {
      log.error("[MilitaryVerificationService] CODEF 2차 호출 실패 - code: {}", resultCode);
      throw new CustomException(MilitaryErrorCode.CODEF_API_FAILED);
    }

    processResult(response.path("data"), user);
    militaryVerificationRepository.delete(verification);
  }

  private void processResult(JsonNode data, User user) {
    String serviceYN = data.path("resServiceYN").asText();
    log.info("[MilitaryVerificationService] 복무 여부: {}", serviceYN);

    if (SERVICE_YN_ACTIVE.equals(serviceYN)) {
      user.completeMilitaryVerification();
      log.info("[MilitaryVerificationService] 군인 인증 완료 - userId: {}", user.getId());
    } else {
      log.info("[MilitaryVerificationService] 현역 복무자 아님 - userId: {}", user.getId());
    }
  }
}
