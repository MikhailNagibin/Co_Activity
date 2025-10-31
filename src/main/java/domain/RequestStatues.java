package domain;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public enum RequestStatues {
  Consideration,
  Accepted,
  Refused
}
