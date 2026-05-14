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

/**
 * 모든 요청에 대해 JWT 액세스 토큰을 검증하고 SecurityContext에 인증 정보를 설정하는 필터입니다.
 *
 * <p>토큰이 만료된 경우 {@link AuthErrorCode#EXPIRED_ACCESS_TOKEN}(AUTH4013)을, 위변조 등 그 외 JWT 예외는 {@link
 * AuthErrorCode#UNAUTHORIZED_TOKEN}(AUTH4012)을 반환합니다.
 *
 * @see JwtProvider
 * @see org.sku.milzip.global.security.CustomUserDetailsService
 * @see org.sku.milzip.global.security.CustomUserDetails
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtProvider jwtProvider;
  private final ObjectMapper objectMapper;

  private static final AntPathMatcher pathMatcher = new AntPathMatcher();

  /**
   * 인증 필터를 건너뛸 경로인지 확인합니다.
   *
   * <p>아래 경로는 인증 없이 접근 가능합니다.
   *
   * <ul>
   *   <li>{@code /api/auth/**} — 로그인, 회원가입, 토큰 재발급 등 인증 API
   *   <li>{@code /v3/api-docs/**}, {@code /swagger-ui/**} — Swagger UI
   *   <li>{@code /error} — 스프링 에러 핸들러
   * </ul>
   *
   * @param request 사용자 요청
   * @return 필터를 건너뛸 경우 {@code true}
   */
  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String uri = request.getRequestURI();
    return pathMatcher.match("/api/auth/**", uri)
        || pathMatcher.match("/v3/api-docs/**", uri)
        || pathMatcher.match("/swagger-ui/**", uri)
        || "/error".equals(uri);
  }

  /**
   * Authorization 헤더의 Bearer 토큰을 추출·검증하고, 유효하면 SecurityContext에 인증 정보를 설정합니다.
   *
   * <p>토큰이 없거나 null인 경우 인증 없이 다음 필터로 진행합니다. 인증이 필요한 엔드포인트는 {@link
   * org.sku.milzip.global.config.SecurityConfig}의 authorizeHttpRequests 설정에서 차단됩니다.
   *
   * @param request 사용자 요청 객체
   * @param response 서버 응답 객체
   * @param filterChain 다음 필터 체인
   * @throws ServletException 서블릿 처리 중 예외
   * @throws IOException 입출력 예외
   */
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

  /**
   * JWT 인증 실패 시 JSON 형식의 에러 응답을 작성합니다.
   *
   * <p>이미 응답이 커밋된 경우({@code response.isCommitted()}) 아무 동작도 수행하지 않습니다.
   *
   * @param response 서버 응답 객체
   * @param errorCode 반환할 인증 에러 코드
   * @throws IOException 응답 스트림 쓰기 중 예외
   */
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
