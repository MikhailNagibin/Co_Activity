package domain;

import java.util.Date;

// todo Не уверен насчет связей таблиц. Проверить это и привязку к бд(не должно ли быть каких-либо аннотаций)

public class User {
  private int id;
  private String login;
  private String username;
  private Date dataOfBirth;
  private String city;
  private String country;
  private String about;
  private int avatar_id;

  public User(int id, String login, String username, Date dataOfBirth, String city,
              String country, String about, int avatar_id) {
    this.id = id;
    this.login = login;
    this.username = username;
    this.dataOfBirth = dataOfBirth;
    this.city = city;
    this.country = country;
    this.about = about;
    this.avatar_id = avatar_id;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getLogin() {
    return login;
  }

  public void setLogin(String login) {
    this.login = login;
  }

  public String getAbout() {
    return about;
  }

  public void setAbout(String about) {
    this.about = about;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public Date getDataOfBirth() {
    return dataOfBirth;
  }

  public void setDataOfBirth(Date dataOfBirth) {
    this.dataOfBirth = dataOfBirth;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public int getAvatar_id() {
    return avatar_id;
  }

  public void setAvatar_id(int avatar_id) {
    this.avatar_id = avatar_id;
  }
}
