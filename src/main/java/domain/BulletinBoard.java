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
  private Room room;
  private String content;
  private User author;
  private Instant updatedAt;
}

