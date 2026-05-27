package org.sku.milzip.domain.review.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VisitWith {
  COUPLE("연인"),
  FAMILY("가족"),
  FRIEND("친구"),
  ALONE("혼자");

  private final String label;
}
