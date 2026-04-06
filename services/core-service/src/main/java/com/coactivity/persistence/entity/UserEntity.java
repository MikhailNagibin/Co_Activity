package com.coactivity.persistence.entity;

import com.coactivity.auth.model.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class UserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "login", nullable = false)
  private String email;

  @Column(name = "email_normalized", nullable = false, unique = true)
  private String emailNormalized;

  @Column(name = "username", nullable = false)
  private String userName;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private UserStatus status;

  @Column(name = "email_verified_at")
  private Instant emailVerifiedAt;

  @Column(name = "birthday")
  private Instant dataOfBirth;

  @Column(name = "country")
  private String country;

  @Column(name = "city")
  private String city;

  @Column(name = "description")
  private String description;

  @Column(name = "avatar_id")
  private Integer avatarId;

  @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
  private List<RoomMemberEntity> memberships = new ArrayList<>();

  @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
  private List<UserNotificationEntity> notificationLinks = new ArrayList<>();
}
