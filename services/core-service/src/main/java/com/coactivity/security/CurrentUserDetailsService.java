package com.coactivity.security;

import com.coactivity.persistence.entity.UserEntity;
import com.coactivity.persistence.repository.UserJpaRepository;
import com.coactivity.service.exception.ResourceNotFoundException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserDetailsService implements UserDetailsService {

  private final UserJpaRepository userJpaRepository;

  public CurrentUserDetailsService(UserJpaRepository userJpaRepository) {
    this.userJpaRepository = userJpaRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    String normalizedEmail = normalizeEmail(username);
    UserEntity user = userJpaRepository.findByEmailNormalized(normalizedEmail)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    return toPrincipal(user);
  }

  public CurrentUserPrincipal loadCurrentUser(Integer userId) {
    UserEntity user = userJpaRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    return toPrincipal(user);
  }

  private CurrentUserPrincipal toPrincipal(UserEntity user) {
    return new CurrentUserPrincipal(
        user.getId(),
        user.getEmail(),
        user.getEmailNormalized(),
        user.getUserName(),
        user.getPasswordHash(),
        user.getStatus(),
        user.getEmailVerifiedAt());
  }

  private String normalizeEmail(String email) {
    return email == null ? null : email.trim().toLowerCase();
  }
}
