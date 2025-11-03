package com.coactivity.domain.entities;

import com.coactivity.domain.enums.Category;
import com.coactivity.domain.enums.Role;
import java.sql.Timestamp;
import java.util.AbstractMap.SimpleEntry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
public class Room {

  private int id;
  private boolean isActive;
  private boolean isVisible;
  private String chatLink;
  private Category category;
  private String name;
  private String description;
  private Timestamp dateOfStartEvent;
  private Timestamp dateOfEndEvent;
  private int ageRating;
  private int frequency;
  private int maximumNumberOfPeople;
  private SimpleEntry<User, Role> users;
}