package org.sku.milzip.domain.user.dto.response;

import org.sku.milzip.domain.user.entity.AuthType;
import org.sku.milzip.domain.user.entity.MilitaryStatus;
import org.sku.milzip.domain.user.entity.User;
import org.sku.milzip.domain.user.entity.UserRole;
import org.sku.milzip.domain.user.entity.UserStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "사용자 정보 응답")
public class UserResponse {

  @Schema(description = "사용자 ID", example = "1")
  private Long id;

  @Schema(description = "이메일", example = "user@example.com")
  private String email;

  @Schema(description = "닉네임", example = "밀집이")
  private String nickname;

  @Schema(description = "역할 (MEMBER / SOLDIER / ADMIN)")
  private UserRole role;

  @Schema(description = "계정 상태 (PENDING_EMAIL / ACTIVE / SUSPENDED)")
  private UserStatus status;

  @Schema(description = "인증 유형 (LOCAL / KAKAO)")
  private AuthType authType;

  @Schema(description = "군인 인증 상태 (NOT_VERIFIED / VERIFIED)")
  private MilitaryStatus militaryStatus;

  @Schema(description = "임시 비밀번호 여부", example = "false")
  private boolean temporaryPassword;

  @Schema(description = "프로필 이미지 URL")
  private String profileImageUrl;

  public static UserResponse from(User user) {
    return new UserResponse(
        user.getId(),
        user.getEmail(),
        user.getNickname(),
        user.getRole(),
        user.getStatus(),
        user.getAuthType(),
        user.getMilitaryStatus(),
        user.isTemporaryPassword(),
        user.getProfileImageUrl());
  }
}
