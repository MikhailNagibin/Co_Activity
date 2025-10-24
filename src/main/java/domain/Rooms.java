package domain;

import java.sql.Timestamp;
// todo Не уверен насчет связей таблиц. Проверить это и привязку к бд(не должно ли быть каких-либо аннотаций)

public class Rooms {
  private int id;
  private boolean isActive;
  private boolean isVisible;
  private String chatLink;
  private Categories category;
  private String name;
  private String descriprion;
  private Timestamp dateOfStartEvent;
  private Timestamp dateOfEndEvent;
  private int ageRating;
  private Users owner;
  private int frequency;
  private int maximumNumberOfPeople;
  private int currentNumberOfPeople;

  public Rooms(int id, boolean isActive, boolean visibility, String chat, Categories category,
               String name, String about, Timestamp dateOfStartEvent,
               Timestamp dateOfEndEvent, int ageRating, Users owner, int frequency,
               int maximumNumberOfPeople, int currentNumberOfPeople) {
    this.id = id;
    this.isActive = isActive;
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
    this.maximumNumberOfPeople = maximumNumberOfPeople;
    this.currentNumberOfPeople = currentNumberOfPeople;
  }

  public boolean isVisible() {
    return isVisible;
  }

  public int getMaximumNumberOfPeople() {
    return maximumNumberOfPeople;
  }

  public void setMaximumNumberOfPeople(int maximumNumberOfPeople) {
    this.maximumNumberOfPeople = maximumNumberOfPeople;
  }

  public int getCurrentNumberOfPeople() {
    return currentNumberOfPeople;
  }

  public void setCurrentNumberOfPeople(int currentNumberOfPeople) {
    this.currentNumberOfPeople = currentNumberOfPeople;
  }

  public boolean isActive() {
    return isActive;
  }

  public void setActive(boolean active) {
    isActive = active;
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

  public Timestamp getDateOfStartEvent() {
    return dateOfStartEvent;
  }

  public void setDateOfStartEvent(Timestamp dateOfStartEvent) {
    this.dateOfStartEvent = dateOfStartEvent;
  }

  public Timestamp getDateOfEndEvent() {
    return dateOfEndEvent;
  }

  public void setDateOfEndEvent(Timestamp dateOfEndEvent) {
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
