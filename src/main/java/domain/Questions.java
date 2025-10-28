package domain;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Questions {
  private int id;
  private Users owner;
  private String question;
}
