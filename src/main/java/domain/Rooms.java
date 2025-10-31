package domain;

import java.sql.Timestamp;
import java.util.AbstractMap.SimpleEntry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
public class Rooms {
  private int id;
  private boolean isActive;
  private boolean isVisible;
  private String chatLink;
  private Categories category;
  private String name;
  private String description; // исправлена опечатка
  private Timestamp dateOfStartEvent;
  private Timestamp dateOfEndEvent;
  private int ageRating;
  private int frequency;
  private int maximumNumberOfPeople;
  private SimpleEntry<Users, Roles> users;
}