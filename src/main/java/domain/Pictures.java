package domain;

// todo Не уверен насчет связей таблиц. Проверить это и привязку к бд(не должно ли быть каких-либо аннотаций)

public class Pictures {
  private Rooms room;
  private int photo_id;
  public Pictures(Rooms room, int photo) {
    this.room = room;
    this.photo_id = photo;
  }

  public Rooms getRoom() {
    return room;
  }

  public void setRoom(Rooms room) {
    this.room = room;
  }

  public int getPhoto() {
    return photo_id;
  }

  public void setPhoto(int photo) {
    this.photo_id = photo;
  }
}
