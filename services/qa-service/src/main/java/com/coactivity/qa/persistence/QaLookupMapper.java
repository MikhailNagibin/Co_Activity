package com.coactivity.qa.persistence;

import com.coactivity.qa.domain.Category;

public final class QaLookupMapper {

  private QaLookupMapper() {
  }

  public static String toDbCategoryName(String rawCategoryName) {
    if (rawCategoryName == null || rawCategoryName.isBlank()) {
      return rawCategoryName;
    }
    String normalized = normalize(rawCategoryName);
    return switch (normalized) {
      case "sport" -> "Sport";
      case "music" -> "Music";
      case "art" -> "Art";
      case "entertainments", "entertainment" -> "Entertainments";
      case "business" -> "Business";
      case "education" -> "Education";
      case "activerecreation", "activerecreationcategory" -> "ActiveRecreation";
      case "passiverecreation", "passiverecreationcategory" -> "PassiveRecreation";
      case "massevent", "isamassevent" -> "MassEvent";
      case "other" -> "Other";
      case "notspecified" -> "NotSpecified";
      default -> rawCategoryName;
    };
  }

  public static Category toCategoryEnum(String dbCategoryName) {
    String normalized = normalize(dbCategoryName);
    return switch (normalized) {
      case "sport" -> Category.SPORT;
      case "music" -> Category.MUSIC;
      case "art" -> Category.ART;
      case "entertainments", "entertainment" -> Category.ENTERTAINMENTS;
      case "business" -> Category.BUSINESS;
      case "education" -> Category.EDUCATION;
      case "activerecreation" -> Category.ACTIVE_RECREATION;
      case "passiverecreation" -> Category.PASSIVE_RECREATION;
      case "massevent", "isamassevent" -> Category.IS_A_MASS_EVENT;
      case "other" -> Category.OTHER;
      case "notspecified" -> Category.NOT_SPECIFIED;
      default -> throw new IllegalArgumentException("Unsupported category in DB: " + dbCategoryName);
    };
  }

  private static String normalize(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Category value is blank");
    }
    return value.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
  }
}
