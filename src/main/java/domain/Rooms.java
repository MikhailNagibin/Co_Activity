package domain;

import java.sql.Timestamp;
import java.util.List;
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
  private String descriprion;
  private Timestamp dateOfStartEvent;
  private Timestamp dateOfEndEvent;
  private int ageRating;
  private Users owner;
  private int frequency;
  private int maximumNumberOfPeople;
  private List<Users> users;
}
