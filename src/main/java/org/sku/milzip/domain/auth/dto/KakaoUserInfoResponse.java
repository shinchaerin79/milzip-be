package org.sku.milzip.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserInfoResponse(
    Long id, @JsonProperty("kakao_account") KakaoAccount kakaoAccount) {

  public record KakaoAccount(
      String email,
      @JsonProperty("email_needs_agreement") Boolean emailNeedsAgreement,
      KakaoProfile profile,
      String name) {

    public record KakaoProfile(String nickname) {}
  }

  public String socialId() {
    return String.valueOf(id);
  }

  public String nickname() {
    if (kakaoAccount == null || kakaoAccount.profile() == null) return null;
    return kakaoAccount.profile().nickname();
  }

  public String email() {
    if (kakaoAccount == null) return null;
    Boolean needsAgreement = kakaoAccount.emailNeedsAgreement();
    if (Boolean.TRUE.equals(needsAgreement)) return null;
    return kakaoAccount.email();
  }

  public String name() {
    if (kakaoAccount == null) return null;
    return kakaoAccount.name();
  }
}
