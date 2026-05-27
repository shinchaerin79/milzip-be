package org.sku.milzip.domain.review.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GoodPoint {
  TASTY("음식이 맛있어요"),
  LARGE_PORTION("양이 많아요"),
  GOOD_VALUE("가성비가 좋아요"),
  GOOD_FOR_SOLO("혼밥하기 좋아요"),
  GOOD_FOR_GROUPS("단체로 오기 좋아요"),
  QUIET("조용하고 좋아요");

  private final String label;
}
