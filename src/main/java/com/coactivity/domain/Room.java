package com.coactivity.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
public class Room {

  private Integer id;
  private boolean isActive;
  private boolean isPublic;
  private String chatLink;
  private Category category;
  private String name;
  private String description;
  private Instant dateOfStartEvent;
  private Instant dateOfEndEvent;
  private int ageRating;
  private Instant frequency;
  private int maximumNumberOfPeople;
  private Map<User, Role> users;
  private List<User> bans;
}
