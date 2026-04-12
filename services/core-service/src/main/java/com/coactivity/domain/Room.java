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
  private String city;
  private String country;
  private Instant dateOfStartEvent;
  private Instant dateOfEndEvent;
  private int ageRating;
  private Instant frequency;
  private int maximumNumberOfPeople;
  private Map<User, Role> users;
  private List<User> bans;

  public Room(Integer id, boolean isActive, boolean isPublic, String chatLink, Category category,
      String name, String description, String city, String country, Instant dateOfStartEvent,
      Instant dateOfEndEvent, int ageRating, Instant frequency, int maximumNumberOfPeople,
      Map<User, Role> users, List<User> bans) {
    this(id, isActive ? RoomStatus.ACTIVE : RoomStatus.INACTIVE, isPublic, chatLink, category,
        name, description, city, country, dateOfStartEvent, dateOfEndEvent, ageRating, frequency,
        maximumNumberOfPeople, users, bans);
  }

  public Room(Integer id, boolean isActive, boolean isPublic, String chatLink, Category category,
      String name, String description, Instant dateOfStartEvent, Instant dateOfEndEvent,
      int ageRating, Instant frequency, int maximumNumberOfPeople, Map<User, Role> users,
      List<User> bans) {
    this(id, isActive, isPublic, chatLink, category, name, description, null, null,
        dateOfStartEvent, dateOfEndEvent, ageRating, frequency, maximumNumberOfPeople, users, bans);
  }

  public Room(Integer id, RoomStatus status, boolean isPublic, String chatLink, Category category,
      String name, String description, String city, String country, Instant dateOfStartEvent,
      Instant dateOfEndEvent, int ageRating, Instant frequency, int maximumNumberOfPeople,
      Map<User, Role> users, List<User> bans) {
    this.id = id;
    this.status = status;
    this.isPublic = isPublic;
    this.chatLink = chatLink;
    this.category = category;
    this.name = name;
    this.description = description;
    this.city = city;
    this.country = country;
    this.dateOfStartEvent = dateOfStartEvent;
    this.dateOfEndEvent = dateOfEndEvent;
    this.ageRating = ageRating;
    this.frequency = frequency;
    this.maximumNumberOfPeople = maximumNumberOfPeople;
    this.users = users;
    this.bans = bans;
  }

  public Room(Integer id, RoomStatus status, boolean isPublic, String chatLink, Category category,
      String name, String description, Instant dateOfStartEvent, Instant dateOfEndEvent,
      int ageRating, Instant frequency, int maximumNumberOfPeople, Map<User, Role> users,
      List<User> bans) {
    this(id, status, isPublic, chatLink, category, name, description, null, null,
        dateOfStartEvent, dateOfEndEvent, ageRating, frequency, maximumNumberOfPeople, users, bans);
  }

  public boolean isActive() {
    return status == RoomStatus.ACTIVE;
  }
}
