package org.sku.milzip.global.openai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.sku.milzip.domain.store.entity.Store;
import org.sku.milzip.domain.store.entity.StoreCategory;
import org.sku.milzip.global.config.properties.OpenAiProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiService {

  private static final String BASE_URL = "https://api.openai.com/v1";

  private final RestClient restClient;
  private final OpenAiProperties openAiProperties;
  private final ObjectMapper objectMapper;

  /** 텍스트를 OpenAI text-embedding-3-small 모델로 임베딩 벡터로 변환합니다. 동일 텍스트 요청은 캐시에서 반환합니다. */
  @Cacheable("embeddings")
  public List<Float> getEmbedding(String text) {
    try {
      Map<String, Object> body =
          Map.of("model", openAiProperties.getEmbeddingModel(), "input", text);

      String response =
          restClient
              .post()
              .uri(BASE_URL + "/embeddings")
              .header("Authorization", "Bearer " + openAiProperties.getApiKey())
              .contentType(MediaType.APPLICATION_JSON)
              .body(body)
              .retrieve()
              .body(String.class);

      JsonNode root = objectMapper.readTree(response);
      JsonNode embeddingNode = root.path("data").get(0).path("embedding");

      List<Float> result = new ArrayList<>(embeddingNode.size());
      for (JsonNode node : embeddingNode) {
        result.add(node.floatValue());
      }
      return result;
    } catch (RestClientException e) {
      log.error("[OpenAiService] 임베딩 API 호출 실패", e);
      throw new RuntimeException("임베딩 생성 실패: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error("[OpenAiService] 임베딩 응답 파싱 실패", e);
      throw new RuntimeException("임베딩 응답 파싱 실패", e);
    }
  }

  /**
   * 이미 선정된 후보 매장 목록에 대해 GPT 추천 이유를 생성합니다. 사용자가 제외를 요청한 매장은 응답에서 제외됩니다.
   *
   * @return storeId → reason 매핑 리스트
   */
  public List<AiRankResult> generateReasons(
      String userQuery,
      String companion,
      List<StoreCategory> requestedCategories,
      List<Store> candidates) {
    try {
      String storesContext = buildStoresContext(candidates);
      String userMessage =
          buildUserMessage(userQuery, companion, requestedCategories, storesContext);

      List<Map<String, String>> messages =
          List.of(
              Map.of("role", "system", "content", buildSystemPrompt()),
              Map.of("role", "user", "content", userMessage));

      Map<String, Object> body =
          Map.of(
              "model",
              openAiProperties.getChatModel(),
              "messages",
              messages,
              "response_format",
              Map.of("type", "json_object"),
              "temperature",
              0.7);

      String response =
          restClient
              .post()
              .uri(BASE_URL + "/chat/completions")
              .header("Authorization", "Bearer " + openAiProperties.getApiKey())
              .contentType(MediaType.APPLICATION_JSON)
              .body(body)
              .retrieve()
              .body(String.class);

      return parseRankResults(response);
    } catch (RestClientException e) {
      log.error("[OpenAiService] 채팅 API 호출 실패", e);
      throw new RuntimeException("GPT 추천 이유 생성 실패: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error("[OpenAiService] 채팅 응답 파싱 실패", e);
      throw new RuntimeException("GPT 응답 파싱 실패", e);
    }
  }

  private String buildSystemPrompt() {
    return """
        당신은 군장병 혜택 매장 추천 전문가입니다.
        후보 매장 중 사용자의 요청에 실제로 부합하는 매장만 선택하여 추천 이유를 작성하세요.

        선택 기준:
        - 사용자가 명시적으로 선택한 카테고리(요청 카테고리 참고)의 매장은 카테고리 자체만을 이유로 제외하지 마세요
          (예: 사용자가 카페 카테고리를 선택했다면 카페 매장은 유지하세요)
        - 음식 카테고리 내에서는 요청한 음식 종류와 맞지 않는 매장을 제외하세요
          (예: 한우구이를 원하는데 족발집·분식집이 후보에 있으면 제외)
        - 사용자가 명시적으로 제외 요청한 음식·분위기·유형의 매장도 제외하세요
        - 매장명, 카테고리, 메뉴 설명을 종합적으로 판단하세요

        추천 이유 작성 규칙(2~3문장, 한국어):
        1) 매장의 특징·대표 메뉴·분위기를 구체적으로 언급하세요 (매장명·메뉴 설명을 근거로)
        2) 할인 혜택을 구체적인 수치/조건으로 명시하세요
           (discountRate가 있으면 "OO% 할인", discountInfo가 있으면 그 내용을 그대로 활용)
        3) 동행자·상황에 비추어 이 매장이 왜 좋은 선택인지 추천 포인트를 덧붙이세요
        과장하지 말고 주어진 정보(메뉴·할인·카테고리) 안에서만 구체적으로 작성하세요.

        반드시 아래 JSON 형식으로만 응답하세요:
        {"recommendations": [{"storeId": 숫자, "reason": "추천 이유"}, ...]}
        """;
  }

  private String buildUserMessage(
      String freeText,
      String companion,
      List<StoreCategory> requestedCategories,
      String storesContext) {
    StringBuilder sb = new StringBuilder();
    sb.append("사용자 요청: ").append(freeText).append("\n");
    if (companion != null && !companion.isBlank()) {
      sb.append("동행자: ").append(companion).append("\n");
    }
    if (requestedCategories != null && !requestedCategories.isEmpty()) {
      String categoryLabels =
          requestedCategories.stream()
              .map(StoreCategory::getLabel)
              .collect(Collectors.joining(", "));
      sb.append("요청 카테고리: ").append(categoryLabels).append("\n");
    }
    sb.append("\n후보 매장 목록:\n").append(storesContext);
    return sb.toString();
  }

  private String buildStoresContext(List<Store> stores) {
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < stores.size(); i++) {
      Store s = stores.get(i);
      String discountInfo =
          s.getBenefits().stream()
              .map(b -> b.getDescription())
              .filter(d -> d != null && !d.isBlank())
              .findFirst()
              .orElse("");
      Integer maxDiscount =
          s.getBenefits().stream()
              .map(b -> b.getDiscountRate())
              .filter(d -> d != null)
              .max(Integer::compareTo)
              .orElse(null);

      if (i > 0) sb.append(",");
      sb.append("{")
          .append("\"id\":")
          .append(s.getId())
          .append(",")
          .append("\"name\":\"")
          .append(escape(s.getName()))
          .append("\",")
          .append("\"category\":\"")
          .append(s.getCategory())
          .append("\",")
          .append("\"address\":\"")
          .append(escape(s.getAddress()))
          .append("\"");
      if (s.getMenuDescription() != null && !s.getMenuDescription().isBlank()) {
        sb.append(",\"menu\":\"").append(escape(s.getMenuDescription())).append("\"");
      }
      if (maxDiscount != null) {
        sb.append(",\"discountRate\":").append(maxDiscount);
      }
      if (!discountInfo.isBlank()) {
        sb.append(",\"discountInfo\":\"").append(escape(discountInfo)).append("\"");
      }
      sb.append("}");
    }
    sb.append("]");
    return sb.toString();
  }

  private List<AiRankResult> parseRankResults(String response) throws Exception {
    JsonNode root = objectMapper.readTree(response);
    String content = root.path("choices").get(0).path("message").path("content").asText();
    log.debug("[OpenAiService] GPT 응답: {}", content);

    JsonNode resultRoot = objectMapper.readTree(content);
    JsonNode recommendations = resultRoot.path("recommendations");

    List<AiRankResult> results = new ArrayList<>();
    for (JsonNode rec : recommendations) {
      long storeId = rec.path("storeId").asLong();
      String reason = rec.path("reason").asText();
      results.add(new AiRankResult(storeId, reason));
    }
    return results;
  }

  private String escape(String s) {
    if (s == null) return "";
    return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
  }

  public record AiRankResult(Long storeId, String reason) {}
}
