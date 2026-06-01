package org.sku.milzip.domain.benefit.repository;

import java.util.List;

import org.sku.milzip.domain.benefit.entity.Tmo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TmoRepository extends JpaRepository<Tmo, Long> {

  @Query("SELECT t FROM Tmo t WHERE t.latitude IS NOT NULL AND t.longitude IS NOT NULL")
  List<Tmo> findAllWithCoordinates();
}
