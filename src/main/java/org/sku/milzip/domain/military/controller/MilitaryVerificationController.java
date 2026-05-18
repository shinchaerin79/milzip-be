package org.sku.milzip.domain.military.controller;

import jakarta.validation.Valid;

import org.sku.milzip.domain.military.dto.MilitaryVerificationRequest;
import org.sku.milzip.domain.military.service.MilitaryVerificationService;
import org.sku.milzip.global.common.BaseResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Military", description = "군인 인증 API")
@RestController
@RequestMapping("/api/military")
@RequiredArgsConstructor
public class MilitaryVerificationController {

  private final MilitaryVerificationService militaryVerificationService;

  @Operation(
      summary = "[ 사용자 | 토큰 O | 군인 인증 요청 (1차) ]",
      description =
          """
          **Purpose**
          - CODEF 병적증명서 API에 1차 요청을 보내 카카오톡 간편인증을 트리거합니다.
          - 성공 시 사용자의 카카오톡으로 인증 요청 알림이 발송됩니다.

          **CODEF 1차 요청 파라미터**

          | 파라미터 | 출처 | 설명 |
          |---|---|---|
          | userName | JWT → DB 조회 | 회원가입 또는 카카오 로그인 시 저장된 본명 |
          | birthDate | 요청값(identity) 앞 6자리 파생 | 생년월일 yymmdd (예: 030709) |
          | identity | 요청값 → 뒷 7자리 RSA 암호화 | 주민등록번호 13자리 입력, 뒷 7자리를 CODEF 공개키로 암호화 후 전송 |
          | identityEncYn | 고정값 "Y" | 암호화 여부 플래그 |
          | phoneNo | 요청값 | 카카오톡에 등록된 전화번호 (카카오 알림 수신용) |
          | addrSido | 요청값 | 주민등록상 시도 (예: 서울특별시) |
          | addrSigungu | 요청값 | 주민등록상 시군구 (예: 강남구) |
          | organization | 고정값 "0001" | 병무청 기관코드 |
          | loginType | 고정값 "6" | 카카오 간편인증 |
          | loginTypeLevel | 고정값 "1" | 카카오톡 (2=카카오페이) |
          | type | 고정값 "1" | 조회 대상: 복무 중인 사람 |

          **CODEF 1차 응답 → DB 저장**
          - CODEF가 CF-03002(2Way 대기)를 반환하면 jobIndex, threadIndex, jti, twoWayTimestamp를 DB에 저장
          - 이 값들은 2차 요청(confirm)에서 사용됩니다

          **Request Body**
          - identity: 주민등록번호 13자리 (숫자만, 예: 0307094080912)
          - phoneNo: 카카오톡 등록 전화번호 (예: 01012345678)
          - addrSido: 주민등록상 시도 (예: 서울특별시)
          - addrSigungu: 주민등록상 시군구 (예: 강남구)

          **Returns**
          - message: "카카오톡으로 인증 요청을 전송했습니다. 카카오톡 승인 후 확인 버튼을 눌러주세요."

          **Note**
          - 카카오톡 알림 승인 후 즉시 `POST /api/military/verifications/confirm` 을 호출해야 합니다
          - 인증 세션은 10분간 유효합니다

          **Error**
          - MIL4001: 이미 인증 완료된 사용자
          - MIL4002: 이름 정보 없음
          - MIL5001: CODEF API 호출 실패
          """)
  @PostMapping("/verifications")
  public BaseResponse<Void> startVerification(
      @AuthenticationPrincipal String email,
      @Valid @RequestBody MilitaryVerificationRequest request) {
    militaryVerificationService.startVerification(email, request);
    return BaseResponse.success("카카오톡으로 인증 요청을 전송했습니다. 카카오톡 승인 후 확인 버튼을 눌러주세요.", null);
  }

  @Operation(
      summary = "[ 사용자 | 토큰 O | 군인 인증 확인 (2차) ]",
      description =
          """
          **Purpose**
          - 카카오톡 간편인증 승인 후 CODEF에 2차 요청을 보내 병적증명서를 조회합니다.
          - 현역 복무 여부를 확인하고 인증 완료 시 role을 SOLDIER로 업데이트합니다.

          **CODEF 2차 요청 파라미터 (Request Body 없음 — 전부 서버가 구성)**

          | 파라미터 | 출처 | 설명 |
          |---|---|---|
          | userName | JWT → DB 조회 | 1차 요청과 동일한 본명 |
          | birthDate | DB 저장값(identity) 앞 6자리 파생 | 1차 요청과 동일 |
          | identity | DB 저장값 → 뒷 7자리 재암호화 | 1차 때 저장한 주민번호를 다시 암호화해서 전송 |
          | identityEncYn | 고정값 "Y" | 암호화 여부 플래그 |
          | phoneNo | DB 저장값 | 1차 요청 시 저장한 전화번호 |
          | addrSido | DB 저장값 | 1차 요청 시 저장한 시도 |
          | addrSigungu | DB 저장값 | 1차 요청 시 저장한 시군구 |
          | simpleAuth | 고정값 "1" | 간편인증 완료 플래그 |
          | is2Way | 고정값 true | 2차 요청 여부 |
          | twoWayInfo.jobIndex | DB 저장값 (1차 응답) | CODEF 1차 응답에서 저장한 값 |
          | twoWayInfo.threadIndex | DB 저장값 (1차 응답) | CODEF 1차 응답에서 저장한 값 |
          | twoWayInfo.jti | DB 저장값 (1차 응답) | CODEF 1차 응답에서 저장한 세션 ID |
          | twoWayInfo.twoWayTimestamp | DB 저장값 (1차 응답) | CODEF 1차 응답에서 저장한 타임스탬프 |

          **CODEF 2차 응답 처리**
          - CF-00000 + resServiceYN="복무를 마치지 않은 사람" → role SOLDIER로 업데이트, 인증 완료
          - CF-00000 + 그 외 resServiceYN → 현역 복무자 아님 (role 변경 없음)
          - CF-12100 → 인터넷 발급 비대상 (주민센터 방문 필요)
          - CF-12701 → 카카오 인증 취소 또는 시간 초과

          **Note**
          - Request Body 없음 — 모든 값은 JWT와 1차 요청 시 DB에 저장된 값에서 자동 조합
          - 카카오톡 승인 후 최대한 빠르게 호출해야 합니다 (세션 만료 방지)

          **Error**
          - MIL4003: 카카오 간편인증 취소 또는 시간 초과
          - MIL4041: 진행 중인 인증 요청 없음 (1차 요청 먼저 필요)
          - MIL4081: 인증 세션 만료 (10분 초과)
          - MIL4221: 인터넷 발급 비대상
          - MIL5001: CODEF API 호출 실패
          """)
  @PostMapping("/verifications/confirm")
  public BaseResponse<Void> confirmVerification(@AuthenticationPrincipal String email) {
    militaryVerificationService.confirmVerification(email);
    return BaseResponse.success("인증이 완료되었습니다.", null);
  }
}
