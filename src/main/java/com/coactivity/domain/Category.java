package com.coactivity.domain;


public enum Category {
  Sport,
  Music,
  Art,
  Entertainments,
  Business,
  Education,
  ActiveRecreation,
  PassiveRecreation,
  isAMassEvent,
  Other,
  NotSpecified;

  public static Category getByIndex(int index) {
    Category[] category = values();
    if (index >= 0 && index < category.length) {
      return category[index];
    }
    throw new IllegalArgumentException("Invalid category index: " + index);
  }
}
