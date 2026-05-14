package org.sku.milzip.domain.user.service;

import java.util.List;

import org.sku.milzip.domain.auth.exception.AuthErrorCode;
import org.sku.milzip.domain.user.dto.UserResponse;
import org.sku.milzip.domain.user.repository.UserRepository;
import org.sku.milzip.global.exception.CustomException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public UserResponse getMyInfo(String email) {
    return userRepository
        .findByEmail(email)
        .map(UserResponse::from)
        .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
  }

  @Transactional(readOnly = true)
  public List<UserResponse> getAllUsers() {
    return userRepository.findAll().stream().map(UserResponse::from).toList();
  }
}
