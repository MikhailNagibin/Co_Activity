package com.coactivity.domain;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class User {

  private Integer id;
  private String login;
  private String username;
  private String password;
  private Instant dataOfBirth;
  private String city;
  private String country;
  private String description;
  private Integer avatarId;
  private List<Room> rooms;
  private List<Notification> notifications;
}