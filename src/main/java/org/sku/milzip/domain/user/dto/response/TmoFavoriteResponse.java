package org.sku.milzip.domain.user.dto.response;

import java.time.LocalDateTime;

import org.sku.milzip.domain.user.entity.TmoFavorite;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "TMO 즐겨찾기 응답")
public class TmoFavoriteResponse {

  @Schema(description = "즐겨찾기 ID")
  private Long id;

  @Schema(description = "TMO ID")
  private Long tmoId;

  @Schema(description = "TMO명")
  private String name;

  @Schema(description = "전화번호")
  private String phone;

  @Schema(description = "주소")
  private String address;

  @Schema(description = "평일 운영 시작")
  private String weekdayStartTime;

  @Schema(description = "평일 운영 종료")
  private String weekdayEndTime;

  @Schema(description = "출장형 여부")
  private boolean isMobile;

  @Schema(description = "즐겨찾기 추가 일시")
  private LocalDateTime createdAt;

  public static TmoFavoriteResponse from(TmoFavorite tf) {
    return TmoFavoriteResponse.builder()
        .id(tf.getId())
        .tmoId(tf.getTmo().getId())
        .name(tf.getTmo().getName())
        .phone(tf.getTmo().getPhone())
        .address(tf.getTmo().getAddress())
        .weekdayStartTime(tf.getTmo().getWeekdayStartTime())
        .weekdayEndTime(tf.getTmo().getWeekdayEndTime())
        .isMobile(tf.getTmo().isMobile())
        .createdAt(tf.getCreatedAt())
        .build();
  }
}
