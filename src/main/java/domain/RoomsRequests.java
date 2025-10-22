package domain;

// todo Не уверен насчет связей таблиц. Проверить это и привязку к бд(не должно ли быть каких-либо аннотаций)

public class RoomsRequests {
  private Users user;
  private Rooms room;
  private RequestStatues status;

  public RoomsRequests(Users user, Rooms room, RequestStatues status) {
    this.user = user;
    this.room = room;
    this.status = status;
  }

  public Users getUser() {
    return user;
  }

  public void setUser(Users user) {
    this.user = user;
  }

  public Rooms getRoom() {
    return room;
  }

  public void setRoom(Rooms room) {
    this.room = room;
  }

  public RequestStatues getStatus() {
    return status;
  }

  public void setStatus(RequestStatues status) {
    this.status = status;
  }
}
