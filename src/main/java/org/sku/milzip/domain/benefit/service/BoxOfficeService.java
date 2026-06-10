package org.sku.milzip.domain.benefit.service;

import java.util.List;

import org.sku.milzip.domain.benefit.dto.response.BoxOfficeItemResponse;
import org.sku.milzip.domain.benefit.repository.WeeklyBoxofficeRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoxOfficeService {

  private static final String REDIS_KEY = "milzip:weekly_boxoffice:latest";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final StringRedisTemplate redisTemplate;
  private final WeeklyBoxofficeRepository weeklyBoxofficeRepository;

  @Transactional(readOnly = true)
  public List<BoxOfficeItemResponse> getWeeklyBoxOffice() {
    log.debug("[BoxOfficeService] 주간 박스오피스 조회 - Redis 캐시 우선 조회");
    // 1. Redis 캐시 우선
    try {
      String cached = redisTemplate.opsForValue().get(REDIS_KEY);
      if (cached != null) {
        List<BoxOfficeItemResponse> result =
            OBJECT_MAPPER.readValue(cached, new TypeReference<List<BoxOfficeItemResponse>>() {});
        log.debug("[BoxOfficeService] Redis 캐시 히트 - {}편", result.size());
        return result;
      }
      log.debug("[BoxOfficeService] Redis 캐시 미스, DB 조회로 폴백");
    } catch (Exception e) {
      log.warn("[BoxOfficeService] Redis 조회 실패, DB로 폴백 - error: {}", e.getMessage());
    }

    // 2. DB 폴백
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
