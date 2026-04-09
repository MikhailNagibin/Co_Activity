package com.coactivity.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class Room {

  private Integer id;
  private RoomStatus status;
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

  public Room(Integer id, boolean isActive, boolean isPublic, String chatLink, Category category,
      String name, String description, Instant dateOfStartEvent, Instant dateOfEndEvent,
      int ageRating, Instant frequency, int maximumNumberOfPeople, Map<User, Role> users,
      List<User> bans) {
    this(id, isActive ? RoomStatus.ACTIVE : RoomStatus.INACTIVE, isPublic, chatLink, category,
        name, description, dateOfStartEvent, dateOfEndEvent, ageRating, frequency,
        maximumNumberOfPeople, users, bans);
  }

  public Room(Integer id, RoomStatus status, boolean isPublic, String chatLink, Category category,
      String name, String description, Instant dateOfStartEvent, Instant dateOfEndEvent,
      int ageRating, Instant frequency, int maximumNumberOfPeople, Map<User, Role> users,
      List<User> bans) {
    this.id = id;
    this.status = status;
    this.isPublic = isPublic;
    this.chatLink = chatLink;
    this.category = category;
    this.name = name;
    this.description = description;
    this.dateOfStartEvent = dateOfStartEvent;
    this.dateOfEndEvent = dateOfEndEvent;
    this.ageRating = ageRating;
    this.frequency = frequency;
    this.maximumNumberOfPeople = maximumNumberOfPeople;
    this.users = users;
    this.bans = bans;
  }

  public boolean isActive() {
    return status == RoomStatus.ACTIVE;
  }
}
