package org.sku.milzip.domain.store.entity;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.sku.milzip.domain.store.dto.request.StoreCreateRequest;
import org.sku.milzip.global.common.BaseTimeEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "stores")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private StoreCategory category;

  @Column(nullable = false)
  private String address;

  private Double latitude;

  private Double longitude;

  @Column(length = 30)
  private String phone;

  @Column(nullable = false)
  private boolean isMilitaryBenefit = false;

  @Column(nullable = false)
  private boolean isBenefitVerified = false;

  @Column(nullable = false)
  private int viewCount = 0;

  private LocalTime openTime;

  private LocalTime closeTime;

  private LocalDateTime closeDate;

  @OneToMany(
      mappedBy = "store",
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  private List<StoreBenefit> benefits = new ArrayList<>();

  @OneToMany(
      mappedBy = "store",
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  private List<StoreImage> images = new ArrayList<>();

  public static Store create(StoreCreateRequest request) {
    Store store = new Store();
    store.name = request.getName();
    store.category = request.getCategory();
    store.address = request.getAddress();
    store.latitude = request.getLatitude();
    store.longitude = request.getLongitude();
    store.phone = request.getPhone();
    store.isMilitaryBenefit = request.isMilitaryBenefit();
    store.isBenefitVerified = request.isBenefitVerified();
    store.openTime = request.getOpenTime();
    store.closeTime = request.getCloseTime();
    store.closeDate = request.getCloseDate();
    return store;
  }

  public void update(StoreCreateRequest request) {
    this.name = request.getName();
    this.category = request.getCategory();
    this.address = request.getAddress();
    this.latitude = request.getLatitude();
    this.longitude = request.getLongitude();
    this.phone = request.getPhone();
    this.isMilitaryBenefit = request.isMilitaryBenefit();
    this.isBenefitVerified = request.isBenefitVerified();
    this.openTime = request.getOpenTime();
    this.closeTime = request.getCloseTime();
    this.closeDate = request.getCloseDate();
  }

  public void incrementViewCount() {
    this.viewCount++;
  }
}
