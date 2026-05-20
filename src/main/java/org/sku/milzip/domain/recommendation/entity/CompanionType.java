package org.sku.milzip.domain.recommendation.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "동행자 유형")
public enum CompanionType {
  FRIEND("친구"),
  COUPLE("연인"),
  FAMILY("가족"),
  ALONE("혼자");

  private final String label;

  CompanionType(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}
