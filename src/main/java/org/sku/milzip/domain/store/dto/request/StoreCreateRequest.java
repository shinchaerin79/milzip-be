package org.sku.milzip.domain.store.dto.request;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.sku.milzip.domain.store.entity.StoreCategory;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "매장 등록/수정 요청")
public class StoreCreateRequest {

  @NotBlank
  @Schema(description = "매장명", example = "아트밸리 자작나무")
  private String name;

  @NotNull @Schema(description = "카테고리 (FOOD / CAFE / LEISURE / ACCOMMODATION / ETC)")
  private StoreCategory category;

  @NotBlank
  @Schema(description = "주소", example = "경기도 포천시 신북면 아트밸리로 42")
  private String address;

  @Schema(description = "위도", example = "37.89257701967812")
  private Double latitude;

  @Schema(description = "경도", example = "127.19789570920469")
  private Double longitude;

  @Schema(description = "전화번호", example = "031-534-9784")
  private String phone;

  @Schema(description = "군장병 혜택 매장 여부", example = "true")
  private boolean militaryBenefit = true;

  @Schema(description = "혜택 인증 여부", example = "false")
  private boolean benefitVerified = false;

  @Schema(description = "영업 시작 시간", example = "09:00")
  private LocalTime openTime;

  @Schema(description = "영업 종료 시간", example = "21:00")
  private LocalTime closeTime;

  @Schema(description = "매장 종료일 (임시 휴업 등)", example = "2025-12-31T00:00:00")
  private LocalDateTime closeDate;

  @Valid
  @Schema(description = "혜택 목록")
  private List<StoreBenefitRequest> benefits = new ArrayList<>();
}
