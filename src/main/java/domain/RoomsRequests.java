package domain;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RoomsRequests {
  private Users user;
  private Rooms room;
  private RequestStatues status;
}
