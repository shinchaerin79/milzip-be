package org.sku.milzip.global.kakao;

import java.util.Optional;

import org.sku.milzip.global.config.properties.KakaoProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoMobilityService {

  private static final String DIRECTIONS_URL = "https://apis-navi.kakaomobility.com/v1/directions";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final RestClient restClient;
  private final KakaoProperties kakaoProperties;

  public record RouteResult(double distanceKm, int durationSeconds) {}

  /** 두 좌표 간 자동차 경로의 실제 거리와 소요 시간을 반환합니다. API 실패 시 Optional.empty() 반환 → 호출자가 fallback 처리합니다. */
  public Optional<RouteResult> getDrivingRoute(
      double originLat, double originLng, double destLat, double destLng) {
    try {
      // X좌표 = 경도(lng), Y좌표 = 위도(lat)
      String origin = originLng + "," + originLat;
      String destination = destLng + "," + destLat;

      String response =
          restClient
              .get()
              .uri(
                  DIRECTIONS_URL
                      + "?origin="
                      + origin
                      + "&destination="
                      + destination
                      + "&priority=RECOMMEND"
                      + "&summary=true")
              .header("Authorization", "KakaoAK " + kakaoProperties.getClientId())
              .header("Content-Type", "application/json")
              .retrieve()
              .body(String.class);

      JsonNode root = OBJECT_MAPPER.readTree(response);
      JsonNode routes = root.path("routes");

      if (!routes.isArray() || routes.isEmpty()) {
        return Optional.empty();
      }

      int resultCode = routes.get(0).path("result_code").asInt(-1);
      if (resultCode != 0) {
        log.warn("[KakaoMobility] 경로 없음 result_code={}", resultCode);
        return Optional.empty();
      }

      JsonNode summary = routes.get(0).path("summary");
      double distanceKm = summary.path("distance").asDouble() / 1000.0;
      int durationSeconds = summary.path("duration").asInt();

      return Optional.of(new RouteResult(distanceKm, durationSeconds));

    } catch (Exception e) {
      log.warn("[KakaoMobility] API 호출 실패, fallback 사용: {}", e.getMessage());
      return Optional.empty();
    }
  }
}
