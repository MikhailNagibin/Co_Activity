package domain;

// todo Не уверен насчет связей таблиц. Проверить это и привязку к бд(не должно ли быть каких-либо аннотаций)

public class Pictures {
  private Rooms roomId;
  private int photo_id;
  public Pictures(Rooms room, int photo) {
    this.roomId = room;
    this.photo_id = photo;
  }

  public Rooms getRoomId() {
    return roomId;
  }

  public void setRoomId(Rooms roomId) {
    this.roomId = roomId;
  }

  public int getPhoto() {
    return photo_id;
  }

  public void setPhoto(int photo) {
    this.photo_id = photo;
  }
}
