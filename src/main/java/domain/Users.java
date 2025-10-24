package domain;

import java.util.Date;

// todo Не уверен насчет связей таблиц. Проверить это и привязку к бд(не должно ли быть каких-либо аннотаций)

public class Users {
  private int id;
  private String login;
  private String username;
  private String password;
  private Date dataOfBirth;
  private String city;
  private String country;
  private String description;
  private int avatar_id;

  public Users(int id, String login, String username, String password, Date dataOfBirth, String city,
               String country, String about, int avatar_id) {
    this.id = id;
    this.login = login;
    this.username = username;
    this.password = password;
    this.dataOfBirth = dataOfBirth;
    this.city = city;
    this.country = country;
    this.description = about;
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

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
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
