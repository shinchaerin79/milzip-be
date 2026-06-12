package org.sku.milzip.domain.benefit.service;

import java.util.List;

import org.sku.milzip.domain.benefit.dto.response.BoxOfficeItemResponse;
import org.sku.milzip.domain.benefit.repository.WeeklyBoxofficeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoxOfficeService {

  private final WeeklyBoxofficeRepository weeklyBoxofficeRepository;

  @Transactional(readOnly = true)
  public List<BoxOfficeItemResponse> getWeeklyBoxOffice() {
    log.debug("[BoxOfficeService] 주간 박스오피스 조회");
    List<BoxOfficeItemResponse> result =
        weeklyBoxofficeRepository
            .findLatestTargetDt()
            .map(weeklyBoxofficeRepository::findByTargetDtOrderByRankAsc)
            .orElse(List.of())
            .stream()
            .map(
                w ->
                    BoxOfficeItemResponse.builder()
                        .rank(w.getRank())
                        .movieCd(w.getMovieCd())
                        .title(w.getTitle())
                        .openDate(w.getOpenDate() != null ? w.getOpenDate().toString() : null)
                        .audienceCount(w.getAudienceCount() != null ? w.getAudienceCount() : 0L)
                        .genre(w.getGenre())
                        .runtimeMinutes(w.getRuntimeMinutes())
                        .posterUrl(w.getPosterUrl())
                        .build())
            .toList();
    log.debug("[BoxOfficeService] DB 조회 완료 - {}편", result.size());
    return result;
  }
}
