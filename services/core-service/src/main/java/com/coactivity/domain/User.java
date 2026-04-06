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
  private String email;
  private String userName;
  private Instant dataOfBirth;
  private String country;
  private String city;
  private String description;
  private Integer avatarId;
  private List<Room> rooms;
  private List<Notification> notifications;
}
