package org.sku.milzip.global.common;

import java.util.List;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "페이지네이션 응답")
public class PageResponse<T> {

  @Schema(description = "콘텐츠 목록")
  private List<T> content;

  @Schema(description = "전체 요소 수", example = "100")
  private long totalElements;

  @Schema(description = "전체 페이지 수", example = "10")
  private int totalPages;

  @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
  private int pageNum;

  @Schema(description = "페이지 크기", example = "10")
  private int pageSize;

  @Schema(description = "마지막 페이지 여부", example = "false")
  private boolean last;

  @Schema(description = "다음 페이지 존재 여부", example = "true")
  private boolean hasNext;

  public static <T> PageResponse<T> from(Page<T> page) {
    return PageResponse.<T>builder()
        .content(page.getContent())
        .totalElements(page.getTotalElements())
        .totalPages(page.getTotalPages())
        .pageNum(page.getNumber())
        .pageSize(page.getSize())
        .last(page.isLast())
        .hasNext(page.hasNext())
        .build();
  }

  public static <T> PageResponse<T> of(
      List<T> content, int pageNum, int pageSize, long totalElements) {
    int totalPages = pageSize == 0 ? 1 : (int) Math.ceil((double) totalElements / pageSize);
    boolean last = (pageNum + 1) >= totalPages;
    return PageResponse.<T>builder()
        .content(content)
        .totalElements(totalElements)
        .totalPages(totalPages)
        .pageNum(pageNum)
        .pageSize(pageSize)
        .last(last)
        .hasNext(!last)
        .build();
  }
}
