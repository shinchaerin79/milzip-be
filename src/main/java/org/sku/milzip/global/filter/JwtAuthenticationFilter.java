package org.sku.milzip.global.filter;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.sku.milzip.domain.auth.exception.AuthErrorCode;
import org.sku.milzip.global.common.BaseResponse;
import org.sku.milzip.global.security.jwt.JwtProvider;
import org.sku.milzip.global.security.jwt.TokenType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtProvider jwtProvider;
  private final ObjectMapper objectMapper;

  private static final AntPathMatcher pathMatcher = new AntPathMatcher();

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String uri = request.getRequestURI();
    return pathMatcher.match("/api/v1/auth/**", uri)
        || pathMatcher.match("/v3/api-docs/**", uri)
        || pathMatcher.match("/swagger-ui/**", uri)
        || "/error".equals(uri);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      String accessToken = jwtProvider.extractAccessToken(request);

      if (accessToken != null && jwtProvider.validateToken(accessToken, TokenType.ACCESS_TOKEN)) {
        String subject = jwtProvider.getSubjectFromToken(accessToken);
        List<GrantedAuthority> authorities = jwtProvider.getAuthoritiesFromToken(accessToken);

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(subject, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }

      filterChain.doFilter(request, response);
    } catch (ExpiredJwtException e) {
      SecurityContextHolder.clearContext();
      log.info("[Auth] 만료된 액세스 토큰 감지, 리프레시 필요");
      writeAuthErrorResponse(response, AuthErrorCode.EXPIRED_ACCESS_TOKEN);
    } catch (JwtException | IllegalArgumentException e) {
      SecurityContextHolder.clearContext();
      log.warn("[Auth] 유효하지 않은 토큰 감지: {}", e.getClass().getSimpleName());
      writeAuthErrorResponse(response, AuthErrorCode.UNAUTHORIZED_TOKEN);
    }
  }

  private void writeAuthErrorResponse(HttpServletResponse response, AuthErrorCode errorCode)
      throws IOException {
    if (response.isCommitted()) return;

    response.setStatus(errorCode.getStatus().value());
    response.setCharacterEncoding("UTF-8");
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    BaseResponse<Object> body =
        BaseResponse.error(errorCode.getStatus().value(), errorCode.getMessage());
    response.getWriter().write(objectMapper.writeValueAsString(body));
    response.getWriter().flush();
  }
}
