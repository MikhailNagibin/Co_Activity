package domain;

import java.util.Date;

// todo Не уверен насчет связей таблиц. Проверить это и привязку к бд(не должно ли быть каких-либо аннотаций)

public class Room {
  private int id;
  private Visibilities visibility;
  private String chat;
  private Categories category;
  private String name;
  private String about;
  private Date dateOfStartEvent;
  private Date dateOfEndEvent;
  private int ageRating;
  private User owner;
  private int frequency;

  public Room(int id, Visibilities visibility, String chat, Categories category,
              String name, String about, Date dateOfStartEvent,
              Date dateOfEndEvent, int ageRating, User owner, int frequency) {
    this.id = id;
    this.visibility = visibility;
    this.chat = chat;
    this.category = category;
    this.name = name;
    this.about = about;
    this.dateOfStartEvent = dateOfStartEvent;
    this.dateOfEndEvent = dateOfEndEvent;
    this.ageRating = ageRating;
    this.owner = owner;
    this.frequency = frequency;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public Visibilities getVisibility() {
    return visibility;
  }

  public void setVisibility(Visibilities visibility) {
    this.visibility = visibility;
  }

  public String getChat() {
    return chat;
  }

  public void setChat(String chat) {
    this.chat = chat;
  }

  public Categories getCategory() {
    return category;
  }

  public void setCategory(Categories category) {
    this.category = category;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAbout() {
    return about;
  }

  public void setAbout(String about) {
    this.about = about;
  }

  public Date getDateOfStartEvent() {
    return dateOfStartEvent;
  }

  public void setDateOfStartEvent(Date dateOfStartEvent) {
    this.dateOfStartEvent = dateOfStartEvent;
  }

  public Date getDateOfEndEvent() {
    return dateOfEndEvent;
  }

  public void setDateOfEndEvent(Date dateOfEndEvent) {
    this.dateOfEndEvent = dateOfEndEvent;
  }

  public int getAgeRating() {
    return ageRating;
  }

  public void setAgeRating(int ageRating) {
    this.ageRating = ageRating;
  }

  public User getOwner() {
    return owner;
  }

  public void setOwner(User owner) {
    this.owner = owner;
  }

  public int getFrequency() {
    return frequency;
  }

  public void setFrequency(int frequency) {
    this.frequency = frequency;
  }
}
