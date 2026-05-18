package org.sku.milzip.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class KakaoUserInfoResponse {

  private Long id;

  @JsonProperty("kakao_account")
  private KakaoAccount kakaoAccount;

  public String socialId() {
    return String.valueOf(id);
  }

  public String nickname() {
    if (kakaoAccount == null || kakaoAccount.getProfile() == null) return null;
    return kakaoAccount.getProfile().getNickname();
  }

  public String email() {
    if (kakaoAccount == null) return null;
    if (Boolean.TRUE.equals(kakaoAccount.getEmailNeedsAgreement())) return null;
    return kakaoAccount.getEmail();
  }

  public String name() {
    if (kakaoAccount == null) return null;
    return kakaoAccount.getName();
  }

  @Getter
  @Setter
  @NoArgsConstructor
  public static class KakaoAccount {

    private String email;

    @JsonProperty("email_needs_agreement")
    private Boolean emailNeedsAgreement;

    private KakaoProfile profile;

    private String name;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class KakaoProfile {
      private String nickname;
    }
  }
}
