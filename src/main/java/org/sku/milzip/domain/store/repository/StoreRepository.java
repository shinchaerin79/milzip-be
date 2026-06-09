package org.sku.milzip.domain.store.repository;

import java.util.List;
import java.util.Optional;

import org.sku.milzip.domain.store.entity.Store;
import org.sku.milzip.domain.store.entity.StoreCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreRepository extends JpaRepository<Store, Long> {

  @Query(
      value = "SELECT DISTINCT s FROM Store s LEFT JOIN FETCH s.benefits",
      countQuery = "SELECT COUNT(s) FROM Store s")
  Page<Store> findAllWithBenefits(Pageable pageable);

  @Query(
      value =
          "SELECT DISTINCT s FROM Store s LEFT JOIN FETCH s.benefits WHERE s.category = :category",
      countQuery = "SELECT COUNT(s) FROM Store s WHERE s.category = :category")
  Page<Store> findByCategoryWithBenefits(
      @Param("category") StoreCategory category, Pageable pageable);

  @Query("SELECT DISTINCT s FROM Store s LEFT JOIN FETCH s.benefits")
  List<Store> findAllWithBenefitsList();

  @Query("SELECT DISTINCT s FROM Store s LEFT JOIN FETCH s.benefits WHERE s.category = :category")
  List<Store> findAllByCategoryWithBenefitsList(@Param("category") StoreCategory category);

  @Query(
      "SELECT s FROM Store s LEFT JOIN FETCH s.benefits WHERE s.latitude IS NOT NULL AND s.longitude IS NOT NULL")
  List<Store> findAllWithLatLng();

  @Query(
      "SELECT s FROM Store s LEFT JOIN FETCH s.benefits WHERE s.category = :category AND s.latitude IS NOT NULL AND s.longitude IS NOT NULL")
  List<Store> findByCategoryWithLatLng(@Param("category") StoreCategory category);

  @Query("SELECT s FROM Store s LEFT JOIN FETCH s.benefits WHERE s.id = :id")
  Optional<Store> findWithBenefitsById(@Param("id") Long id);

  @Query(
      "SELECT DISTINCT s FROM Store s LEFT JOIN FETCH s.benefits WHERE s.isMilitaryBenefit = true AND s.latitude IS NOT NULL AND s.longitude IS NOT NULL")
  List<Store> findMilitaryBenefitWithLatLng();

  @Query(
      "SELECT DISTINCT s FROM Store s LEFT JOIN FETCH s.benefits WHERE s.isMilitaryBenefit = true AND s.category = :category AND s.latitude IS NOT NULL AND s.longitude IS NOT NULL")
  List<Store> findMilitaryBenefitByCategoryWithLatLng(@Param("category") StoreCategory category);

  @Query(
      "SELECT DISTINCT s FROM Store s LEFT JOIN FETCH s.benefits WHERE s.isMilitaryBenefit = true")
  List<Store> findAllMilitaryBenefit();

  @Query(
      "SELECT DISTINCT s FROM Store s LEFT JOIN FETCH s.benefits WHERE s.isMilitaryBenefit = true AND s.category = :category")
  List<Store> findMilitaryBenefitByCategory(@Param("category") StoreCategory category);

  @Query(
      "SELECT DISTINCT s FROM Store s LEFT JOIN FETCH s.benefits WHERE s.category IN :categories")
  List<Store> findAllByCategoriesWithBenefitsList(
      @Param("categories") List<StoreCategory> categories);

  @Query(
      "SELECT s FROM Store s LEFT JOIN FETCH s.benefits WHERE s.category IN :categories AND s.latitude IS NOT NULL AND s.longitude IS NOT NULL")
  List<Store> findByCategoriesWithLatLng(@Param("categories") List<StoreCategory> categories);

  @Query("SELECT DISTINCT s FROM Store s LEFT JOIN FETCH s.benefits WHERE s.id IN :ids")
  List<Store> findAllByIdWithBenefits(@Param("ids") List<Long> ids);

  @Query("SELECT s FROM Store s LEFT JOIN FETCH s.benefits ORDER BY s.viewCount DESC")
  List<Store> findTopByViewCount(Pageable pageable);

  boolean existsByNameAndAddress(String name, String address);
}
