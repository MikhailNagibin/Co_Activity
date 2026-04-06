package com.coactivity.security;

import com.coactivity.auth.model.UserStatus;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class CurrentUserPrincipal implements UserDetails, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private final Integer userId;
  private final String email;
  private final String emailNormalized;
  private final String userName;
  private final String passwordHash;
  private final UserStatus status;
  private final Instant emailVerifiedAt;

  public CurrentUserPrincipal(Integer userId, String email, String emailNormalized, String userName,
      String passwordHash, UserStatus status, Instant emailVerifiedAt) {
    this.userId = userId;
    this.email = email;
    this.emailNormalized = emailNormalized;
    this.userName = userName;
    this.passwordHash = passwordHash;
    this.status = status;
    this.emailVerifiedAt = emailVerifiedAt;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of();
  }

  @Override
  public String getPassword() {
    return passwordHash;
  }

  @Override
  public String getUsername() {
    return emailNormalized;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return status != UserStatus.DISABLED;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return status == UserStatus.ACTIVE;
  }
}
