package org.sku.milzip.global.codef;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Cipher;

import org.sku.milzip.domain.military.exception.MilitaryErrorCode;
import org.sku.milzip.global.exception.CustomException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodefService {

  private final CodefProperties codefProperties;
  private final CodefTokenManager codefTokenManager;
  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  public JsonNode callMilitary(Map<String, Object> requestBody) {
    String url = codefProperties.getMilitaryUrl();
    log.info("[CodefService] 병적증명서 API 요청 - URL: {}", url);
    log.info("[CodefService] 병적증명서 API 요청 바디: {}", requestBody);
    try {
      String token = codefTokenManager.getToken();
      log.info(
          "[CodefService] 사용할 토큰 (앞 20자): {}",
          token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null");

      ResponseEntity<String> response =
          restClient
              .post()
              .uri(url)
              .header("Authorization", "Bearer " + token)
              .contentType(MediaType.APPLICATION_JSON)
              .body(requestBody)
              .retrieve()
              .toEntity(String.class);

      log.info("[CodefService] 병적증명서 API HTTP 상태코드: {}", response.getStatusCode());
      log.info("[CodefService] 병적증명서 API 응답 헤더: {}", response.getHeaders());
      String responseBody = response.getBody();
      log.info("[CodefService] 병적증명서 API 원본 응답: {}", responseBody);
      if (responseBody == null) {
        log.error("[CodefService] 병적증명서 API 응답 바디가 null");
        throw new CustomException(MilitaryErrorCode.CODEF_API_FAILED);
      }
      String decoded = URLDecoder.decode(responseBody, StandardCharsets.UTF_8);
      log.info("[CodefService] 병적증명서 API 디코딩 응답: {}", decoded);
      return objectMapper.readTree(decoded);
    } catch (CustomException e) {
      throw e;
    } catch (Exception e) {
      log.error(
          "[CodefService] 병적증명서 API 호출 실패 - 예외 타입: {}, 메시지: {}",
          e.getClass().getSimpleName(),
          e.getMessage(),
          e);
      throw new CustomException(MilitaryErrorCode.CODEF_API_FAILED);
    }
  }

  public Map<String, Object> buildFirstRequest(
      String userName, String identity, String phoneNo, String addrSido, String addrSigungu) {
    String birthDate = identity.substring(0, 6);
    String encryptedBackId = encryptIdentity(identity.substring(6));

    Map<String, Object> map = new LinkedHashMap<>();
    map.put("organization", "0001");
    map.put("loginType", "6");
    map.put("loginTypeLevel", "1");
    map.put("userName", userName);
    map.put("birthDate", birthDate);
    map.put("identity", encryptedBackId);
    map.put("identityEncYn", "Y");
    map.put("phoneNo", phoneNo);
    map.put("addrSido", addrSido);
    map.put("addrSigungu", addrSigungu);
    map.put("type", "1");
    map.put("branchOfMilitary", "0");
    map.put("serviceStatus", "0");
    map.put("rank", "0");
    map.put("unitOfSpecialty", "0");
    map.put("field", "0");
    map.put("militaryIDNo", "0");
    map.put("dischargeReason", "0");
    map.put("originDataYN", "0");
    return map;
  }

  public Map<String, Object> buildSecondRequest(
      String userName,
      String identity,
      String phoneNo,
      String addrSido,
      String addrSigungu,
      int jobIndex,
      int threadIndex,
      String jti,
      long twoWayTimestamp) {
    Map<String, Object> map = buildFirstRequest(userName, identity, phoneNo, addrSido, addrSigungu);
    map.put("simpleAuth", "1");
    map.put("is2Way", true);

    Map<String, Object> twoWayInfo = new LinkedHashMap<>();
    twoWayInfo.put("jobIndex", jobIndex);
    twoWayInfo.put("threadIndex", threadIndex);
    twoWayInfo.put("jti", jti);
    twoWayInfo.put("twoWayTimestamp", twoWayTimestamp);
    map.put("twoWayInfo", twoWayInfo);

    return map;
  }

  private String encryptIdentity(String backId) {
    try {
      PublicKey publicKey = fetchCodefPublicKey();
      Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
      cipher.init(Cipher.ENCRYPT_MODE, publicKey);
      byte[] encrypted = cipher.doFinal(backId.getBytes(StandardCharsets.UTF_8));
      String base64 = Base64.getEncoder().encodeToString(encrypted);
      return URLEncoder.encode(base64, StandardCharsets.UTF_8);
    } catch (CustomException e) {
      throw e;
    } catch (Exception e) {
      log.error("[CodefService] 주민번호 RSA 암호화 실패", e);
      throw new CustomException(MilitaryErrorCode.CODEF_API_FAILED);
    }
  }

  private PublicKey fetchCodefPublicKey() {
    try {
      byte[] keyBytes = Base64.getDecoder().decode(codefProperties.getPublicKey());
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      return keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
    } catch (Exception e) {
      log.error("[CodefService] CODEF RSA 공개키 파싱 실패", e);
      throw new CustomException(MilitaryErrorCode.CODEF_API_FAILED);
    }
  }
}
