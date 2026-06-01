package org.sku.milzip.domain.benefit.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "박스오피스 영화 응답")
public class BoxOfficeItemResponse {

  @Schema(description = "순위", example = "1")
  private int rank;

  @Schema(description = "영화 코드", example = "20250001")
  private String movieCd;

  @Schema(description = "영화명", example = "살목지")
  private String title;

  @Schema(description = "개봉일", example = "2025-05-01")
  private String openDate;

  @Schema(description = "누적 관객수", example = "600000")
  private long audienceCount;

  @Schema(description = "장르", example = "스릴러")
  private String genre;

  @Schema(description = "상영시간 (분)", example = "119")
  private Integer runtimeMinutes;

  @Schema(description = "포스터 이미지 URL")
  private String posterUrl;
}
