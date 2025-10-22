package domain;

// todo Не уверен насчет связей таблиц. Проверить это и привязку к бд(не должно ли быть каких-либо аннотаций)

public class RoomMembers {
  private Rooms room;
  private Users user;
  private Roles role;

  public RoomMembers(Rooms room, Users user, Roles role) {
    this.room = room;
    this.user = user;
    this.role = role;
  }

  public Rooms getRoom() {
    return room;
  }

  public void setRoom(Rooms room) {
    this.room = room;
  }

  public Users getUser() {
    return user;
  }

  public void setUser(Users user) {
    this.user = user;
  }

  public Roles getRole() {
    return role;
  }

  public void setRole(Roles role) {
    this.role = role;
  }
}
