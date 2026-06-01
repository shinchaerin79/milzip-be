package org.sku.milzip.domain.benefit.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.sku.milzip.domain.benefit.entity.WeeklyBoxoffice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WeeklyBoxofficeRepository extends JpaRepository<WeeklyBoxoffice, Long> {

  @Query("SELECT MAX(w.targetDt) FROM WeeklyBoxoffice w")
  Optional<LocalDate> findLatestTargetDt();

  List<WeeklyBoxoffice> findByTargetDtOrderByRankAsc(LocalDate targetDt);
}
