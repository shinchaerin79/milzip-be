package org.sku.milzip.domain.benefit.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.sku.milzip.domain.benefit.dto.request.AmusementParkBenefitRequest;
import org.sku.milzip.domain.benefit.dto.request.BenefitCreateRequest;
import org.sku.milzip.domain.benefit.dto.request.MovieBenefitRequest;
import org.sku.milzip.domain.benefit.dto.request.SelfDevelopmentBenefitRequest;
import org.sku.milzip.global.common.BaseTimeEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "benefits")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Benefit extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BenefitType benefitType;

  @Column(nullable = false)
  private String title;

  @Column(length = 1000)
  private String description;

  @Column(length = 1000)
  private String imageUrl;

  // 공통 - 영화 혜택
  private String cinemaChain;

  // 공통 - 놀이공원 혜택
  private String region;
  private String location;
  private LocalDate validFrom;
  private LocalDate validUntil;
  private Integer originalPrice;
  private Integer discountedPrice;
  private String discountDescription;

  @Column(length = 500)
  private String verificationMethod;

  // 공통 - 자기계발 혜택
  private String category;

  @Column(length = 1000)
  private String applyUrl;

  @Column(length = 2000)
  private String supportType;

  private String superviseInst;

  public static Benefit create(BenefitCreateRequest request) {
    Benefit benefit = new Benefit();
    benefit.benefitType = request.getBenefitType();
    benefit.title = request.getTitle();
    benefit.description = request.getDescription();
    benefit.imageUrl = request.getImageUrl();
    benefit.cinemaChain = request.getCinemaChain();
    benefit.region = request.getRegion();
    benefit.location = request.getLocation();
    benefit.validFrom = request.getValidFrom();
    benefit.validUntil = request.getValidUntil();
    benefit.originalPrice = request.getOriginalPrice();
    benefit.discountedPrice = request.getDiscountedPrice();
    benefit.discountDescription = request.getDiscountDescription();
    benefit.verificationMethod = request.getVerificationMethod();
    benefit.category = request.getCategory();
    benefit.applyUrl = request.getApplyUrl();
    benefit.supportType = request.getSupportType();
    return benefit;
  }

  public void update(BenefitCreateRequest request) {
    this.title = request.getTitle();
    this.description = request.getDescription();
    this.imageUrl = request.getImageUrl();
    this.cinemaChain = request.getCinemaChain();
    this.region = request.getRegion();
    this.location = request.getLocation();
    this.validFrom = request.getValidFrom();
    this.validUntil = request.getValidUntil();
    this.originalPrice = request.getOriginalPrice();
    this.discountedPrice = request.getDiscountedPrice();
    this.discountDescription = request.getDiscountDescription();
    this.verificationMethod = request.getVerificationMethod();
    this.category = request.getCategory();
    this.applyUrl = request.getApplyUrl();
    this.supportType = request.getSupportType();
  }

  public static Benefit createMovieBenefit(MovieBenefitRequest request) {
    Benefit benefit = new Benefit();
    benefit.benefitType = BenefitType.MOVIE_BENEFIT;
    benefit.title = request.getCinemaChain();
    benefit.description = request.getDescription();
    benefit.cinemaChain = request.getCinemaChain();
    return benefit;
  }

  public void updateMovieBenefit(MovieBenefitRequest request) {
    this.title = request.getCinemaChain();
    this.description = request.getDescription();
    this.cinemaChain = request.getCinemaChain();
  }

  public static Benefit createAmusementPark(AmusementParkBenefitRequest request) {
    Benefit benefit = new Benefit();
    benefit.benefitType = BenefitType.AMUSEMENT_PARK;
    benefit.title = request.getTitle();
    benefit.description = request.getDescription();
    benefit.imageUrl = request.getImageUrl();
    benefit.region = request.getRegion();
    benefit.location = request.getLocation();
    benefit.validFrom = request.getValidFrom();
    benefit.validUntil = request.getValidUntil();
    benefit.originalPrice = request.getOriginalPrice();
    benefit.discountedPrice = request.getDiscountedPrice();
    benefit.discountDescription = request.getDiscountDescription();
    benefit.verificationMethod = request.getVerificationMethod();
    return benefit;
  }

  public void updateAmusementPark(AmusementParkBenefitRequest request) {
    this.title = request.getTitle();
    this.description = request.getDescription();
    this.imageUrl = request.getImageUrl();
    this.region = request.getRegion();
    this.location = request.getLocation();
    this.validFrom = request.getValidFrom();
    this.validUntil = request.getValidUntil();
    this.originalPrice = request.getOriginalPrice();
    this.discountedPrice = request.getDiscountedPrice();
    this.discountDescription = request.getDiscountDescription();
    this.verificationMethod = request.getVerificationMethod();
  }

  public static Benefit createSelfDevelopment(SelfDevelopmentBenefitRequest request) {
    Benefit benefit = new Benefit();
    benefit.benefitType = BenefitType.SELF_DEVELOPMENT;
    benefit.title = request.getTitle();
    benefit.description = request.getDescription();
    benefit.imageUrl = request.getImageUrl();
    benefit.category = request.getCategory();
    benefit.applyUrl = request.getApplyUrl();
    benefit.supportType = request.getSupportType();
    return benefit;
  }

  public void updateSelfDevelopment(SelfDevelopmentBenefitRequest request) {
    this.title = request.getTitle();
    this.description = request.getDescription();
    this.imageUrl = request.getImageUrl();
    this.category = request.getCategory();
    this.applyUrl = request.getApplyUrl();
    this.supportType = request.getSupportType();
  }
}
