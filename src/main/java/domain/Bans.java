package domain;

import java.util.Date;

// todo Не уверен насчет связей таблиц. Проверить это и привязку к бд(не должно ли быть каких-либо аннотаций)

public class Bans {
  private Users user;
  private Rooms room;
  private int DurationOfBan;
  private Date dateOfBan;

  public Bans(Users user, Rooms room, int time, Date date) {
    this.user = user;
    this.room = room;
    this.DurationOfBan = time;
    this.dateOfBan = date;
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

  public int getDurationOfBan() {
    return DurationOfBan;
  }

  public void setDurationOfBan(int durationOfBan) {
    this.DurationOfBan = durationOfBan;
  }

  public Date getDateOfBan() {
    return dateOfBan;
  }

  public void setDateOfBan(Date dateOfBan) {
    this.dateOfBan = dateOfBan;
  }
}
