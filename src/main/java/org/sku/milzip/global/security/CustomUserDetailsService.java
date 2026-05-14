package org.sku.milzip.global.security;

import org.sku.milzip.domain.auth.exception.AuthErrorCode;
import org.sku.milzip.domain.user.entity.User;
import org.sku.milzip.domain.user.repository.UserRepository;
import org.sku.milzip.global.exception.CustomException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
    return new CustomUserDetails(user);
  }
}
