package domain;

// todo Не уверен насчет связей таблиц. Проверить это и привязку к бд(не должно ли быть каких-либо аннотаций)

public class Applications {
  private User user;
  private Room room;
  private Statues status;

  public Applications(User user, Room room, Statues status) {
    this.user = user;
    this.room = room;
    this.status = status;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Room getRoom() {
    return room;
  }

  public void setRoom(Room room) {
    this.room = room;
  }

  public Statues getStatus() {
    return status;
  }

  public void setStatus(Statues status) {
    this.status = status;
  }
}
