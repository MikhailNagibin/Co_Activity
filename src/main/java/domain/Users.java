package domain;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Users {
  private int id;
  private String login;
  private String username;
  private String password;
  private Instant dataOfBirth;
  private String city;
  private String country;
  private String description;
  private int avatarId;
  private List<Rooms> rooms;
}