package org.sku.milzip.domain.user.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  private String password;

  @Column(nullable = false)
  private String nickname;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserRole role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AuthType authType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MilitaryStatus militaryStatus;

  private String socialId;

  private String name;

  @Column(nullable = false)
  private boolean temporaryPassword;

  private LocalDateTime emailVerifiedAt;

  private String profileImageUrl;

  public static User ofLocal(String email, String encodedPassword, String nickname, String name) {
    User user = new User();
    user.email = email;
    user.password = encodedPassword;
    user.nickname = nickname;
    user.name = name;
    user.role = UserRole.MEMBER;
    user.status = UserStatus.PENDING_EMAIL;
    user.authType = AuthType.LOCAL;
    user.militaryStatus = MilitaryStatus.NOT_VERIFIED;
    user.temporaryPassword = false;
    return user;
  }

  public static User ofKakao(String socialId, String nickname, String email, String name) {
    User user = new User();
    user.socialId = socialId;
    user.email = email != null ? email : "kakao_" + socialId + "@milzip.kakao";
    user.nickname = nickname;
    user.name = name;
    user.role = UserRole.MEMBER;
    user.status = UserStatus.ACTIVE;
    user.authType = AuthType.KAKAO;
    user.militaryStatus = MilitaryStatus.NOT_VERIFIED;
    user.temporaryPassword = false;
    return user;
  }

  public void activateEmail() {
    this.status = UserStatus.ACTIVE;
    this.emailVerifiedAt = LocalDateTime.now();
  }

  public void changePassword(String encodedPassword) {
    this.password = encodedPassword;
    this.temporaryPassword = false;
  }

  public void applyTemporaryPassword(String encodedPassword) {
    this.password = encodedPassword;
    this.temporaryPassword = true;
  }

  public void completeMilitaryVerification() {
    this.militaryStatus = MilitaryStatus.VERIFIED;
    this.role = UserRole.SOLDIER;
  }

  public void updateProfileImage(String profileImageUrl) {
    this.profileImageUrl = profileImageUrl;
  }
}
