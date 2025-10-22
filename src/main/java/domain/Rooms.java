package domain;

import java.util.Date;

// todo Не уверен насчет связей таблиц. Проверить это и привязку к бд(не должно ли быть каких-либо аннотаций)

public class Rooms {
  private int id;
  private boolean isVisible;
  private String chatLink;
  private Categories category;
  private String name;
  private String descriprion;
  private Date dateOfStartEvent;
  private Date dateOfEndEvent;
  private int ageRating;
  private Users owner;
  private int frequency;

  public Rooms(int id, boolean visibility, String chat, Categories category,
               String name, String about, Date dateOfStartEvent,
               Date dateOfEndEvent, int ageRating, Users owner, int frequency) {
    this.id = id;
    this.isVisible = visibility;
    this.chatLink = chat;
    this.category = category;
    this.name = name;
    this.descriprion = about;
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

  public boolean getVisible() {
    return isVisible;
  }

  public void setVisible(boolean visible) {
    this.isVisible = visible;
  }

  public String getChatLink() {
    return chatLink;
  }

  public void setChatLink(String chatLink) {
    this.chatLink = chatLink;
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

  public String getDescriprion() {
    return descriprion;
  }

  public void setDescriprion(String descriprion) {
    this.descriprion = descriprion;
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

  public Users getOwner() {
    return owner;
  }

  public void setOwner(Users owner) {
    this.owner = owner;
  }

  public int getFrequency() {
    return frequency;
  }

  public void setFrequency(int frequency) {
    this.frequency = frequency;
  }
}
