package domain;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class BulletinBoard {
  private int id;
  private Rooms room;
  private String content;
  private Users author;
  private Instant updatedAt;
}

