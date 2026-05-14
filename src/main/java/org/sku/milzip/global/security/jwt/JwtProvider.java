package org.sku.milzip.global.security.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import jakarta.servlet.http.HttpServletRequest;

import org.sku.milzip.global.config.properties.JwtProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtProvider {

  private static final String ROLES_CLAIM = "roles";
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtProperties jwtProperties;

  public String generateAccessToken(
      String subject, Collection<? extends GrantedAuthority> authorities) {
    return Jwts.builder()
        .subject(subject)
        .claim(ROLES_CLAIM, authorities.stream().map(GrantedAuthority::getAuthority).toList())
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessExpiration()))
        .signWith(getSecretKey())
        .compact();
  }

  public String generateRefreshToken(String subject) {
    return Jwts.builder()
        .subject(subject)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshExpiration()))
        .signWith(getSecretKey())
        .compact();
  }

  public String extractAccessToken(HttpServletRequest request) {
    String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (StringUtils.hasText(bearer) && bearer.startsWith(BEARER_PREFIX)) {
      return bearer.substring(BEARER_PREFIX.length());
    }
    return null;
  }

  /** 토큰 유효성 검증. 만료 시 ExpiredJwtException, 위변조 시 JwtException 을 던집니다. */
  public boolean validateToken(String token, TokenType type) {
    Jwts.parser().verifyWith(getSecretKey()).build().parseSignedClaims(token);
    return true;
  }

  public String getSubjectFromToken(String token) {
    return getClaims(token).getSubject();
  }

  public List<GrantedAuthority> getAuthoritiesFromToken(String token) {
    List<String> roles = getClaims(token).get(ROLES_CLAIM, List.class);
    if (roles == null) return List.of();
    return roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
  }

  private Claims getClaims(String token) {
    return Jwts.parser().verifyWith(getSecretKey()).build().parseSignedClaims(token).getPayload();
  }

  private SecretKey getSecretKey() {
    return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
  }
}
