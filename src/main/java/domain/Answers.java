package domain;

// todo Не уверен насчет связей таблиц. Проверить это и привязку к бд(не должно ли быть каких-либо аннотаций)

public class Answers {
  private int id;
  private Questions question;
  private int previousAnswerId;
  private String answer;
  private User owner;

  public Answers(int id, Questions question, int previousAnswerId, String answer,
                 User owner) {
    this.id = id;
    this.question = question;
    this.previousAnswerId = previousAnswerId;
    this.answer = answer;
    this.owner = owner;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public Questions getQuestion() {
    return question;
  }

  public void setQuestion(Questions question) {
    this.question = question;
  }

  public int getPreviousAnswerId() {
    return previousAnswerId;
  }

  public void setPreviousAnswerId(int previousAnswerId) {
    this.previousAnswerId = previousAnswerId;
  }

  public String getAnswer() {
    return answer;
  }

  public void setAnswer(String answer) {
    this.answer = answer;
  }

  public User getOwner() {
    return owner;
  }

  public void setOwner(User owner) {
    this.owner = owner;
  }
}
