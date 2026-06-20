package org.sku.milzip.domain.recommendation.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.sku.milzip.domain.recommendation.entity.CompanionType;
import org.sku.milzip.domain.store.entity.StoreCategory;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "상황별 AI 맞춤 추천 요청")
public class AiRecommendationRequest {

  @Schema(
      description = "동행자 (FRIEND: 친구 / COUPLE: 연인 / FAMILY: 가족 / ALONE: 혼자)",
      example = "FAMILY")
  private CompanionType companion;

  @Schema(
      description = "방문할 카테고리 (1~3개 선택 가능, 미입력 시 전체)",
      example = "[\"FOOD\", \"CAFE\"]",
      allowableValues = {"FOOD", "CAFE", "LEISURE", "ACCOMMODATION", "ETC"})
  @Size(min = 1, max = 3, message = "카테고리는 1개 이상 3개 이하로 선택해주세요.")
  private List<StoreCategory> categories;

  @Schema(description = "자유 텍스트 요청", example = "치킨 먹고 싶은데 근처 치킨집 추천해줘")
  @NotBlank(message = "요청 내용을 입력해주세요.")
  private String freeText;

  @Schema(description = "현재 위치 위도 (입력 시 거리 정보 포함)", example = "37.9162")
  private Double lat;

  @Schema(description = "현재 위치 경도 (입력 시 거리 정보 포함)", example = "127.1948")
  private Double lng;
}
