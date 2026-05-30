package org.sku.milzip.domain.user.dto.response;

import java.time.LocalDateTime;

import org.sku.milzip.domain.store.entity.StoreCategory;
import org.sku.milzip.domain.user.entity.Favorite;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "즐겨찾기 응답")
public class FavoriteResponse {

  @Schema(description = "즐겨찾기 ID")
  private Long id;

  @Schema(description = "매장 ID")
  private Long storeId;

  @Schema(description = "매장명")
  private String storeName;

  @Schema(description = "카테고리")
  private StoreCategory category;

  @Schema(description = "주소")
  private String address;

  @Schema(description = "즐겨찾기 추가 일시")
  private LocalDateTime createdAt;

  public static FavoriteResponse from(Favorite favorite) {
    return FavoriteResponse.builder()
        .id(favorite.getId())
        .storeId(favorite.getStore().getId())
        .storeName(favorite.getStore().getName())
        .category(favorite.getStore().getCategory())
        .address(favorite.getStore().getAddress())
        .createdAt(favorite.getCreatedAt())
        .build();
  }
}
