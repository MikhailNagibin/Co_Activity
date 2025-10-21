package domain;

// todo Не уверен насчет связей таблиц. Проверить это и привязку к бд(не должно ли быть каких-либо аннотаций)

public class Statues {
  private int id;
  private String status;

  public Statues(int id, String status) {
    this.id = id;
    this.status = status;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
