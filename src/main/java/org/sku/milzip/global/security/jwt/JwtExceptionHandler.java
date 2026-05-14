package org.sku.milzip.global.security.jwt;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.sku.milzip.domain.auth.exception.AuthErrorCode;
import org.sku.milzip.global.common.BaseResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class JwtExceptionHandler implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    AuthErrorCode errorCode = AuthErrorCode.AUTHENTICATION_REQUIRED;

    Object attribute = request.getAttribute("jwtErrorCode");
    if (attribute instanceof AuthErrorCode code) {
      errorCode = code;
    }

    sendErrorResponse(response, errorCode);
  }

  private void sendErrorResponse(HttpServletResponse response, AuthErrorCode errorCode)
      throws IOException {
    response.setStatus(errorCode.getStatus().value());
    response.setContentType("application/json;charset=UTF-8");
    response
        .getWriter()
        .write(
            objectMapper.writeValueAsString(
                BaseResponse.error(errorCode.getStatus().value(), errorCode.getMessage())));
  }
}
