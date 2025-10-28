package domain;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Answers {
  private int id;
  private Questions question;
  private Answers previousAnswer;
  private String answer;
  private Users owner;
}
