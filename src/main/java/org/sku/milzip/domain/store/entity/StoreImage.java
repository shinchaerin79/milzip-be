package org.sku.milzip.domain.store.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "store_images")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreImage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "store_id", nullable = false)
  private Store store;

  @Column(nullable = false, length = 1000)
  private String imageUrl;

  @Column(nullable = false)
  private int displayOrder;

  public static StoreImage of(Store store, String imageUrl, int displayOrder) {
    StoreImage image = new StoreImage();
    image.store = store;
    image.imageUrl = imageUrl;
    image.displayOrder = displayOrder;
    return image;
  }
}
