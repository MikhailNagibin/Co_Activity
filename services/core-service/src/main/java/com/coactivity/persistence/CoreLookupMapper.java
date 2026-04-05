package com.coactivity.persistence;

import com.coactivity.domain.Category;
import com.coactivity.domain.Notification;
import com.coactivity.domain.RequestStatus;
import com.coactivity.domain.Role;

public final class CoreLookupMapper {

  private CoreLookupMapper() {
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

  public static Category toCategory(String dbCategoryName) {
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

  public static Role toRole(String dbRoleName) {
    String normalized = normalize(dbRoleName);
    return switch (normalized) {
      case "owner" -> Role.OWNER;
      case "admin" -> Role.ADMIN;
      case "participant" -> Role.PARTICIPANT;
      default -> throw new IllegalArgumentException("Unknown role in DB: " + dbRoleName);
    };
  }

  public static String toDbRoleName(Role role) {
    return role.toString();
  }

  public static Notification toNotification(String dbNotificationName) {
    return Notification.fromValue(dbNotificationName);
  }

  public static String toDbNotificationName(Notification notification) {
    return notification.toString();
  }

  public static RequestStatus toRequestStatus(String dbStatus) {
    return RequestStatus.fromDatabase(dbStatus);
  }

  public static String toDbRequestStatus(RequestStatus requestStatus) {
    return requestStatus.toDatabaseValue();
  }

  private static String normalize(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Lookup value is blank");
    }
    return value.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
  }
}
