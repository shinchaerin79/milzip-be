package org.sku.milzip.domain.benefit.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "weekly_boxoffice")
@NoArgsConstructor
public class WeeklyBoxoffice {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private LocalDate targetDt;

  @Column(nullable = false)
  private int rank;

  @Column(nullable = false)
  private String movieCd;

  @Column(nullable = false)
  private String title;

  private LocalDate openDate;

  private Long audienceCount;

  private String genre;

  private Integer runtimeMinutes;

  private String posterUrl;

  @Column(nullable = false)
  private LocalDateTime updatedAt;
}
