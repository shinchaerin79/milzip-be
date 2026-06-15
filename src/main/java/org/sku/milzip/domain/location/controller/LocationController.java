package org.sku.milzip.domain.location.controller;

import org.sku.milzip.domain.location.dto.response.ReverseGeocodeResponse;
import org.sku.milzip.global.common.BaseResponse;
import org.sku.milzip.global.kakao.KakaoLocalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Location", description = "위치 관련 API")
@RestController
@RequestMapping("/location")
@RequiredArgsConstructor
public class LocationController {

  private final KakaoLocalService kakaoLocalService;

  @Operation(
      summary = "GPS 좌표 → 한국어 주소 변환",
      description = "위도/경도를 받아 '구 동' 형태의 한국어 주소를 반환합니다. 인증 불필요.")
  @GetMapping("/reverse-geocode")
  public ResponseEntity<BaseResponse<ReverseGeocodeResponse>> reverseGeocode(
      @RequestParam double lat, @RequestParam double lng) {

    String address = kakaoLocalService.reverseGeocode(lat, lng).orElse("");

    return ResponseEntity.ok(BaseResponse.success(new ReverseGeocodeResponse(address)));
  }
}
