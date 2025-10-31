package domain;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Pictures {
  private Rooms roomId;
  private int photoId;
}
