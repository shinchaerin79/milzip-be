package org.sku.milzip.domain.benefit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.sku.milzip.global.common.BaseTimeEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "tmos")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tmo extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  private String phone;

  private String weekdayStartTime;

  private String weekdayEndTime;

  private String weekendStartTime;

  private String weekendEndTime;

  @Column(length = 500)
  private String locationDescription;

  @Column(length = 500)
  private String note;

  @Column(nullable = false)
  private boolean isMobile = false;

  private Double latitude;

  private Double longitude;

  private String address;
}
