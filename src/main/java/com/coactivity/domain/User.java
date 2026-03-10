package com.coactivity.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "login", unique = true, nullable = false)
  private String login;

  @Column(name = "user_name")
  private String userName;

  @Column(name = "password", nullable = false)
  private String password;

  @Column(name = "data_of_birth")
  private Instant dataOfBirth;

  @Column(name = "country")
  private String country;

  @Column(name = "city")
  private String city;

  @Column(name = "description", length = 1000)
  private String description;

  @Column(name = "avatar_id")
  private Integer avatarId;

  @OneToMany(mappedBy = "user")
  private List<RoomMember> rooms;

  @OneToMany(mappedBy = "user")
  private List<UserNotification> notifications;
}
