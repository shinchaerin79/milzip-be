package org.sku.milzip.domain.benefit.dto.response;

import org.sku.milzip.domain.benefit.entity.Tmo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "TMO 응답")
public class TmoResponse {

  @Schema(description = "TMO ID")
  private Long id;

  @Schema(description = "TMO명", example = "서울 TMO")
  private String name;

  @Schema(description = "전화번호", example = "02-748-7458")
  private String phone;

  @Schema(description = "평일 운영 시작", example = "07:00")
  private String weekdayStartTime;

  @Schema(description = "평일 운영 종료", example = "20:30")
  private String weekdayEndTime;

  @Schema(description = "주말 운영 시작", example = "07:00")
  private String weekendStartTime;

  @Schema(description = "주말 운영 종료", example = "20:30")
  private String weekendEndTime;

  @Schema(description = "위치 설명")
  private String locationDescription;

  @Schema(description = "비고")
  private String note;

  @Schema(description = "출장형 여부")
  private boolean isMobile;

  @Schema(description = "위도")
  private Double latitude;

  @Schema(description = "경도")
  private Double longitude;

  @Schema(description = "주소")
  private String address;

  @Schema(description = "현재 위치와의 거리 (km)")
  private Double distanceKm;

  public static TmoResponse from(Tmo tmo) {
    return TmoResponse.builder()
        .id(tmo.getId())
        .name(tmo.getName())
        .phone(tmo.getPhone())
        .weekdayStartTime(tmo.getWeekdayStartTime())
        .weekdayEndTime(tmo.getWeekdayEndTime())
        .weekendStartTime(tmo.getWeekendStartTime())
        .weekendEndTime(tmo.getWeekendEndTime())
        .locationDescription(tmo.getLocationDescription())
        .note(tmo.getNote())
        .isMobile(tmo.isMobile())
        .latitude(tmo.getLatitude())
        .longitude(tmo.getLongitude())
        .address(tmo.getAddress())
        .build();
  }

  public static TmoResponse from(Tmo tmo, double distanceKm) {
    return TmoResponse.builder()
        .id(tmo.getId())
        .name(tmo.getName())
        .phone(tmo.getPhone())
        .weekdayStartTime(tmo.getWeekdayStartTime())
        .weekdayEndTime(tmo.getWeekdayEndTime())
        .weekendStartTime(tmo.getWeekendStartTime())
        .weekendEndTime(tmo.getWeekendEndTime())
        .locationDescription(tmo.getLocationDescription())
        .note(tmo.getNote())
        .isMobile(tmo.isMobile())
        .latitude(tmo.getLatitude())
        .longitude(tmo.getLongitude())
        .address(tmo.getAddress())
        .distanceKm(distanceKm)
        .build();
  }
}
