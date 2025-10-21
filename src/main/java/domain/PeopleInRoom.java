package domain;

// todo Не уверен насчет связей таблиц. Проверить это и привязку к бд(не должно ли быть каких-либо аннотаций)

public class PeopleInRoom {
  private Room room;
  private User user;
  private Roles role;

  public PeopleInRoom(Room room, User user, Roles role) {
    this.room = room;
    this.user = user;
    this.role = role;
  }

  public Room getRoom() {
    return room;
  }

  public void setRoom(Room room) {
    this.room = room;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Roles getRole() {
    return role;
  }

  public void setRole(Roles role) {
    this.role = role;
  }
}
