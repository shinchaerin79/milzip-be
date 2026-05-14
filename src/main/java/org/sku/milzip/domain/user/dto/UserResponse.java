package org.sku.milzip.domain.user.dto;

import org.sku.milzip.domain.user.entity.AuthType;
import org.sku.milzip.domain.user.entity.MilitaryStatus;
import org.sku.milzip.domain.user.entity.User;
import org.sku.milzip.domain.user.entity.UserRole;
import org.sku.milzip.domain.user.entity.UserStatus;

public record UserResponse(
    Long id,
    String email,
    String nickname,
    UserRole role,
    UserStatus status,
    AuthType authType,
    MilitaryStatus militaryStatus,
    boolean temporaryPassword) {

  public static UserResponse from(User user) {
    return new UserResponse(
        user.getId(),
        user.getEmail(),
        user.getNickname(),
        user.getRole(),
        user.getStatus(),
        user.getAuthType(),
        user.getMilitaryStatus(),
        user.isTemporaryPassword());
  }
}
