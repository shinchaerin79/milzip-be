package org.sku.milzip.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import lombok.RequiredArgsConstructor;

@Configuration
@Profile("dev")
@RequiredArgsConstructor
public class SwaggerSecurityConfig {

  @Value("${swagger.auth.username:milzip}")
  private String username;

  @Value("${swagger.auth.password:milzip1234}")
  private String password;

  private final PasswordEncoder passwordEncoder;

  @Bean
  @Order(1)
  public SecurityFilterChain swaggerFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
        .csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .httpBasic(basic -> {});

    return http.build();
  }

  @Bean
  public UserDetailsService swaggerUserDetailsService() {
    return new InMemoryUserDetailsManager(
        User.builder()
            .username(username)
            .password(passwordEncoder.encode(password))
            .roles("SWAGGER")
            .build());
  }
}
