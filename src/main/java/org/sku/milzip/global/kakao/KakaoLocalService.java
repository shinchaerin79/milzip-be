package org.sku.milzip.global.kakao;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.sku.milzip.domain.location.dto.response.GeocodeResponse;
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
public class KakaoLocalService {

  private static final String COORD2REGION_URL =
      "https://dapi.kakao.com/v2/local/geo/coord2regioncode.json";
  private static final String SEARCH_ADDRESS_URL =
      "https://dapi.kakao.com/v2/local/search/address.json";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final RestClient restClient;
  private final KakaoProperties kakaoProperties;

  /** 좌표를 받아 "구 동" 형태의 한국어 주소 반환. API 실패 시 Optional.empty() */
  public Optional<String> reverseGeocode(double lat, double lng) {
    try {
      String response =
          restClient
              .get()
              .uri(COORD2REGION_URL + "?x=" + lng + "&y=" + lat)
              .header("Authorization", "KakaoAK " + kakaoProperties.getLocalApiKey())
              .retrieve()
              .body(String.class);

      JsonNode documents = OBJECT_MAPPER.readTree(response).path("documents");
      if (!documents.isArray() || documents.isEmpty()) {
        return Optional.empty();
      }

      JsonNode doc = documents.get(0);
      String region2 = doc.path("region_2depth_name").asText("").trim();
      String region3 = doc.path("region_3depth_name").asText("").trim();

      String address = region3.isEmpty() ? region2 : region2 + " " + region3;
      return address.isBlank() ? Optional.empty() : Optional.of(address);

    } catch (Exception e) {
      log.warn("[KakaoLocal] 역지오코딩 실패: {}", e.getMessage());
      return Optional.empty();
    }
  }

  /** 주소 텍스트로 좌표 목록 반환. API 실패 시 빈 리스트 반환 */
  public List<GeocodeResponse> geocode(String query) {
    try {
      String response =
          restClient
              .get()
              .uri(SEARCH_ADDRESS_URL + "?query=" + query)
              .header("Authorization", "KakaoAK " + kakaoProperties.getLocalApiKey())
              .retrieve()
              .body(String.class);

      JsonNode documents = OBJECT_MAPPER.readTree(response).path("documents");
      if (!documents.isArray() || documents.isEmpty()) {
        return Collections.emptyList();
      }

      List<GeocodeResponse> results = new java.util.ArrayList<>();
      for (JsonNode doc : documents) {
        String address = doc.path("address_name").asText("").trim();
        double lat = doc.path("y").asDouble(0);
        double lng = doc.path("x").asDouble(0);
        if (!address.isBlank() && lat != 0 && lng != 0) {
          results.add(new GeocodeResponse(address, lat, lng));
        }
      }
      return results;

    } catch (Exception e) {
      log.warn("[KakaoLocal] 지오코딩 실패: {}", e.getMessage());
      return Collections.emptyList();
    }
  }
}
