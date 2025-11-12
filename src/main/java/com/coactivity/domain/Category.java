package com.coactivity.domain;


public enum Category {
  SPORT,
  MUSIC,
  ART,
  ENTERTAINMENTS,
  BUSINESS,
  EDUCATION,
  ACTIVE_RECREATION,
  PASSIVE_RECREATION,
  IS_A_MASS_EVENT,
  OTHER,
  NOT_SPECIFIED;

  public static Category getByIndex(int index) {
    Category[] category = values();
    if (index >= 0 && index < category.length) {
      return category[index];
    }
    throw new IllegalArgumentException("Invalid category index: " + index);
  }
}
