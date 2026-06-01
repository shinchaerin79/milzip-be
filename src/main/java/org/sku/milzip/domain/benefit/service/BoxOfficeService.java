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
    // 1. Redis 캐시 우선
    try {
      String cached = redisTemplate.opsForValue().get(REDIS_KEY);
      if (cached != null) {
        return OBJECT_MAPPER.readValue(cached, new TypeReference<List<BoxOfficeItemResponse>>() {});
      }
    } catch (Exception e) {
      log.warn("[BoxOffice] Redis 조회 실패, DB로 폴백: {}", e.getMessage());
    }

    // 2. DB 폴백
    return weeklyBoxofficeRepository
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
  }
}
