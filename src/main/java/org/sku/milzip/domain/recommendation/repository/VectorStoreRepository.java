package org.sku.milzip.domain.recommendation.repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.sku.milzip.domain.store.entity.Store;
import org.sku.milzip.domain.store.entity.StoreCategory;
import org.sku.milzip.domain.store.repository.StoreRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class VectorStoreRepository {

  private static final int DEFAULT_CANDIDATE_LIMIT = 20;

  private final JdbcTemplate jdbcTemplate;
  private final StoreRepository storeRepository;

  /**
   * pgvector 코사인 유사도로 가장 유사한 매장 ID 목록을 조회합니다.
   *
   * @param embedding 쿼리 임베딩 벡터
   * @return 유사도 순으로 정렬된 매장 목록 (embedding이 없는 매장 제외)
   */
  @Transactional(readOnly = true)
  public List<Store> findSimilarStores(List<Float> embedding) {
    return findSimilarStores(embedding, DEFAULT_CANDIDATE_LIMIT);
  }

  @Transactional(readOnly = true)
  public List<Store> findSimilarStores(List<Float> embedding, int limit) {
    String vectorStr = toVectorString(embedding);

    List<Long> ids =
        jdbcTemplate.queryForList(
            "SELECT id FROM stores WHERE embedding IS NOT NULL "
                + "ORDER BY embedding <=> (?)::vector LIMIT ?",
            Long.class,
            vectorStr,
            limit);

    if (ids.isEmpty()) {
      log.debug("[VectorStoreRepository] 임베딩 유사도 검색 결과 없음");
      return List.of();
    }

    // ID 순서 유지를 위해 Map으로 인덱싱 후 재정렬
    List<Store> stores = storeRepository.findAllByIdWithBenefits(ids);
    Map<Long, Store> storeMap = new LinkedHashMap<>();
    for (Store s : stores) {
      storeMap.put(s.getId(), s);
    }

    List<Store> ordered = new ArrayList<>(ids.size());
    for (Long id : ids) {
      Store s = storeMap.get(id);
      if (s != null) ordered.add(s);
    }
    log.debug("[VectorStoreRepository] 유사 매장 {}건 조회", ordered.size());
    return ordered;
  }

  /**
   * 특정 카테고리 내에서 pgvector 코사인 유사도로 유사한 매장을 조회합니다.
   *
   * @param embedding 쿼리 임베딩 벡터
   * @param category 필터링할 StoreCategory
   * @param limit 최대 결과 수
   */
  @Transactional(readOnly = true)
  public List<Store> findSimilarStoresByCategory(
      List<Float> embedding, StoreCategory category, int limit) {
    String vectorStr = toVectorString(embedding);

    List<Long> ids =
        jdbcTemplate.queryForList(
            "SELECT id FROM stores WHERE embedding IS NOT NULL AND category = ? "
                + "ORDER BY embedding <=> (?)::vector LIMIT ?",
            Long.class,
            category.name(),
            vectorStr,
            limit);

    if (ids.isEmpty()) {
      log.debug("[VectorStoreRepository] 카테고리={} 검색 결과 없음", category);
      return List.of();
    }

    List<Store> stores = storeRepository.findAllByIdWithBenefits(ids);
    Map<Long, Store> storeMap = new LinkedHashMap<>();
    for (Store s : stores) storeMap.put(s.getId(), s);

    List<Store> ordered = new ArrayList<>(ids.size());
    for (Long id : ids) {
      Store s = storeMap.get(id);
      if (s != null) ordered.add(s);
    }
    log.debug("[VectorStoreRepository] 카테고리={} 유사 매장 {}건 조회", category, ordered.size());
    return ordered;
  }

  private String toVectorString(List<Float> embedding) {
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < embedding.size(); i++) {
      if (i > 0) sb.append(",");
      sb.append(embedding.get(i));
    }
    sb.append("]");
    return sb.toString();
  }
}
