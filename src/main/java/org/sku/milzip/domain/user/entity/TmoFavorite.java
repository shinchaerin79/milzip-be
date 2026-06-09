package org.sku.milzip.domain.user.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.sku.milzip.domain.benefit.entity.Tmo;
import org.sku.milzip.global.common.BaseTimeEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "tmo_favorites",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "tmo_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TmoFavorite extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tmo_id", nullable = false)
  private Tmo tmo;

  public static TmoFavorite create(User user, Tmo tmo) {
    TmoFavorite tf = new TmoFavorite();
    tf.user = user;
    tf.tmo = tmo;
    return tf;
  }
}
