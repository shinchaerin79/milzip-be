package org.sku.milzip.domain.review.entity;

import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.BatchSize;
import org.sku.milzip.domain.review.dto.request.ReviewCreateRequest;
import org.sku.milzip.domain.store.entity.Store;
import org.sku.milzip.domain.user.entity.User;
import org.sku.milzip.global.common.BaseTimeEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "reviews")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "store_id", nullable = false)
  private Store store;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false)
  private int rating;

  @Enumerated(EnumType.STRING)
  @Column(nullable = true)
  private BenefitStatus benefitStatus; // SOLDIER만 입력 (MEMBER는 null)

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private VisitType visitType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private WaitTime waitTime;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private VisitPurpose visitPurpose;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private VisitWith visitWith;

  @ElementCollection(fetch = FetchType.EAGER)
  @BatchSize(size = 100)
  @CollectionTable(name = "review_good_points", joinColumns = @JoinColumn(name = "review_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "good_point")
  private List<GoodPoint> goodPoints;

  @Column(length = 500)
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ReviewStatus status;

  public static Review create(Store store, User user, ReviewCreateRequest request) {
    Review review = new Review();
    review.store = store;
    review.user = user;
    review.rating = request.getRating();
    review.benefitStatus = request.getBenefitStatus();
    review.visitType = request.getVisitType();
    review.waitTime = request.getWaitTime();
    review.visitPurpose = request.getVisitPurpose();
    review.visitWith = request.getVisitWith();
    review.goodPoints = request.getGoodPoints();
    review.content = request.getContent();
    review.status = ReviewStatus.VISIBLE;
    return review;
  }

  public void update(ReviewCreateRequest request) {
    this.rating = request.getRating();
    this.benefitStatus = request.getBenefitStatus();
    this.visitType = request.getVisitType();
    this.waitTime = request.getWaitTime();
    this.visitPurpose = request.getVisitPurpose();
    this.visitWith = request.getVisitWith();
    this.goodPoints = request.getGoodPoints();
    this.content = request.getContent();
  }

  public void updateStatus(ReviewStatus status) {
    this.status = status;
  }
}
