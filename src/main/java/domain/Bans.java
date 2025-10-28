package domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Bans {
  private Users user;
  private Rooms room;
  private Duration durationOfBan; //в секундах
  private Instant dateOfBan;
}
